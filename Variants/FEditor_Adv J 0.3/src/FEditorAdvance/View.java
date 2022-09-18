/*
 *  FE Editor - GBA Fire Emblem (U) ROM editor
 *
 *  Copyright (C) 2008-2011 Hextator,
 *  hextator (AIM) hextator@gmail.com (MSN)
 *
 *  Major thanks to Zahlman (AIM/MSN: zahlman@gmail.com) for optimization,
 *  organization and modularity improvements.
 * 
 *  Thanks to Camtech for some contributions to this file.
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License version 3
 *  as published by the Free Software Foundation
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 *
 *  <Description> This class contains the logic code for interfacing
 *  between the user and the editing components
 */

package FEditorAdvance;

import org.jdesktop.application.Action;
import org.jdesktop.application.Application;
import org.jdesktop.application.ResourceMap;
import org.jdesktop.application.FrameView;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeEvent;
import java.awt.event.KeyEvent;
import java.awt.event.InputEvent;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.Dimension;
import javax.swing.Timer;
import javax.swing.LayoutStyle;
import javax.swing.Icon;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.SwingWorker.StateValue;
import javax.swing.JMenuItem;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JSeparator;
import javax.swing.GroupLayout;
import javax.swing.SwingConstants;
import javax.swing.KeyStroke;
import javax.swing.ActionMap;
import javax.swing.JOptionPane;
import Controls.Process;
import Controls.ProcessComponent;
import Model.Game;
import Model.Listener;
import Model.PointerArray;

/**
 * The application's main frame.
 */
public class View extends FrameView implements PropertyChangeListener {
	private App app;
	private Process worker;

	private final Timer busyIconTimer;
	private final Icon idleIcon;
	private final Icon[] busyIcons = new Icon[15];
	private int busyIconIndex = 0;

	private boolean showDialogWhenComplete;

	private JDialog aboutBox;

	private Game game;

	private JProgressBar progressBar = new JProgressBar();
	private JLabel statusAnimationLabel = new JLabel();
	private JLabel statusMessageLabel = new JLabel();

	private Editor<? extends PointerArray> dummyEditor =
		new Editor<PointerArray>(this);
	private Editor<? extends PointerArray> currentEditor = dummyEditor;

	private JMenuItem closeMenuItem;
	private JMenuItem openMenuItem;
	private JMenuItem saveAsMenuItem;
	private JMenuItem saveCopyMenuItem;
	private JMenuItem saveMenuItem;
	private JMenuItem exitMenuItem;

	private JMenuItem classAnimationCreatorMenuItem;
	private JMenuItem classAnimationManagerMenuItem;
	private JMenuItem portraitEditorMenuItem;
	private JMenuItem spellAnimationInserterMenuItem;
	private JMenuItem textEditorMenuItem;

	private JMenuItem aboutMenuItem;
	private JMenuItem debugOutMenuItem;

	private FloatingNotepad openDebugOut = null;

	public View(App app) {
		super(app);
		this.app = app;

		initComponents();

		// Swing disagrees with the loading of the new default editor
		// if it occurs at the time that it will be with this
		// potentially convenient but rather unnecessary line here
		//loadGame(Game.open(path));

		// status bar initialization - message timeout, idle icon and busy animation, etc
		ResourceMap resourceMap = getResourceMap();
		//int messageTimeout = resourceMap.getInteger("StatusBar.messageTimeout");
		int busyAnimationRate = resourceMap.getInteger("StatusBar.busyAnimationRate");
		for (int i = 0; i < busyIcons.length; i++) {
			busyIcons[i] = resourceMap.getIcon("StatusBar.busyIcons[" + i + "]");
		}
		busyIconTimer = new Timer(busyAnimationRate, new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				busyIconIndex = (busyIconIndex + 1) % busyIcons.length;
				statusAnimationLabel.setIcon(busyIcons[busyIconIndex]);
			}
		});
		idleIcon = resourceMap.getIcon("StatusBar.idleIcon");
		statusAnimationLabel.setIcon(idleIcon);
		progressBar.setVisible(false);
	}

	public void open(String path) {
		unloadGame();
		loadGame(Game.open(path));
		if (game != null) { setDefaultEditor(); }
	}

	private void doSaveCloseDialog(boolean quit, Listener postAction) {
		if (game == null) {
			postAction.onUpdate();
			return;
		}

		String ws = "%s without saving";
		String sa = "Save and %s";

		String qc = quit ? "quit" : "close";
		String QC = quit ? "Quit" : "Close";

		boolean unsaved = !(game.isSaved() && currentEditor.isSaved());

		String[] choices = unsaved ?
		new String[] {
			String.format(ws, QC),
			String.format(sa, qc),
			"Cancel"
		} :
		new String [] {
			QC,
			"Cancel"
		};

		int cancelOption = choices.length - 1;

		String message = unsaved
			? "There are unsaved changes to this ROM.\nWhat would you like to do?"
			: String.format("Are you sure you want to %s?", quit ? "quit" : "close the ROM");

		int result = JOptionPane.showOptionDialog(
			null, 
			message,
			unsaved ? "Save changes?" : "Confirm",
			JOptionPane.DEFAULT_OPTION,
			JOptionPane.QUESTION_MESSAGE,
			null,
			choices,
			choices[cancelOption]
		);

		// If the user dismisses the dialog, treat that as Cancel.
		if (result == -1 || result == cancelOption) { return; }

		// Check if "save and close"/"save and quit" was selected.
		if (unsaved && result == 1) {
			doSave(null, true, postAction);
		} else {
			postAction.onUpdate();
		}
	}

	private void doSave(
		final String fileName,
		final boolean includeMetadata,
		final Listener postAction
	) {
		process(
			false, // don't show dialogs
			new Process(
				false, // can't cancel
				// First, move data from pointer array to ROM
				new ProcessComponent(1, 0, "Saving...") {
					@Override
					protected boolean iterate(int i) {
						currentEditor.teardown();
						// Hextator sez: This fixes a null
						// pointer exception
						currentEditor.setup(game);
						return false;
					}
				},
				// Then write from ROM to disk
				new ProcessComponent(1, 0, "Saving...") {
					@Override
					protected boolean iterate(int i) {
						game.save(
							fileName,
							getResourceMap().getString("Application.version"),
							includeMetadata
						);
						return false;
					}
				}
			) {
				@Override
				public void finish() {
					if (postAction != null) { postAction.onUpdate(); }
				}
			}
		);
	}

	private void unloadGame() {
		game = null;
		try {
			openDebugOut.flushPad();
		} catch (Exception e) {}

		classAnimationCreatorMenuItem.setEnabled(false);
		classAnimationManagerMenuItem.setEnabled(false);
		portraitEditorMenuItem.setEnabled(false);
		spellAnimationInserterMenuItem.setEnabled(false);
		textEditorMenuItem.setEnabled(false);

		saveMenuItem.setEnabled(false);
		saveAsMenuItem.setEnabled(false);
		saveCopyMenuItem.setEnabled(false);
		closeMenuItem.setEnabled(false);

		// Replace the binary editor with a dummy editor now
		// that the game is closed
		setDummyEditor();
	}

	private void loadGame(Game g) {
		if (g == null) { return; }
		game = g;

		saveMenuItem.setEnabled(true);
		saveAsMenuItem.setEnabled(true);
		saveCopyMenuItem.setEnabled(true);
		closeMenuItem.setEnabled(true);

		// NOTE: Currently, all editors are supported for all games, but
		// in general, menu items should be disabled when the game does not
		// support the corresponding editor, instead of allowing the user to 
		// select the menu item only to receive an error message.
		classAnimationCreatorMenuItem.setEnabled(true);
		classAnimationManagerMenuItem.setEnabled(true);
		portraitEditorMenuItem.setEnabled(true);
		spellAnimationInserterMenuItem.setEnabled(true);
		textEditorMenuItem.setEnabled(true);

		// Replace dummy editor with binary editor now that a game is
		// open
		//setDefaultEditor();
	}

	@Action
	public void showAboutBox() {
		if (aboutBox == null) {
			JFrame mainFrame = app.getMainFrame();
			aboutBox = new AboutBox(mainFrame);
			aboutBox.setLocationRelativeTo(mainFrame);
		}
		app.show(aboutBox);
	}

	@Action
	public void exitMenuItemExecute() {
		doSaveCloseDialog(true, new Listener() {
			@Override
			public void onUpdate() {
				System.exit(0);
			}
		});
	}

	@Action
	public void openMenuItemExecute() {
		doSaveCloseDialog(false, new Listener() {
			@Override
			public void onUpdate() { open(null); }
		});
	}

	@Action
	public void saveMenuItemExecute() {
		if (game != null) { doSave(null, true, null); }
	}

	@Action
	public void closeMenuItemExecute() {
		doSaveCloseDialog(false, new Listener() {
			@Override
			public void onUpdate() { unloadGame(); }
		}); 
	}

	@Action
	public void saveAsMenuItemExecute() {
		final String fileName = CommonDialogs.showSaveAsDialog();
		// Don't forward a null fileName to doSave() since then it will save
		// over the original file, when the user was asking to cancel.
		if (fileName != null) { doSave(fileName, true, null); }
	}

	@Action
	public void saveCopyMenuItemExecute() {
		final String fileName = CommonDialogs.showSaveAsDialog();
		// Don't forward a null fileName to doSave() since then it will save
		// over the original file, when the user was asking to cancel.
		if (fileName != null) { doSave(fileName, false, null); }
	}

	private ClassAnimationCreator cac = new ClassAnimationCreator(this);

	@Action
	public void classAnimationCreatorMenuItemExecute() {
		setEditor(cac, "アニメーションクリエイター");
	}

	private SpellAnimationInserter sai = new SpellAnimationInserter(this);

	@Action
	public void spellAnimationInserterMenuItemExecute() {
		setEditor(sai, "魔法エフェクト挿入");
	}

	private ClassAnimationManager cam = new ClassAnimationManager(this);

	@Action
	public void classAnimationManagerMenuItemExecute() {
		setEditor(cam, "アニメーションマネージャー");
	}

	private TextEditor ted = new TextEditor(this);

	@Action
	public void textEditorMenuItemExecute() {
		setEditor(ted, "テキストエディター＆ビューア");
	}

	private PortraitEditor pe = new PortraitEditor(this);

	@Action
	public void portraitEditorMenuItemExecute() {
		setEditor(pe, "顔画像編集");
	}

	private void debugOutMenuClosed() {
		debugOutMenuItem.setEnabled(true);
	}

	@Action
	public void debugOutMenuItemExecute() {
		boolean problem = false;
		try {
			ModelessFrame.newInstance(
				openDebugOut = new FloatingNotepad(
					"デバッグ出力",
					App.getSysOutStream(),
					false
				),
				null,
				new Listener() {
					@Override
					public void onUpdate() {
						debugOutMenuClosed();
					}
				}
			);
		} catch (Exception e) { problem = true; }
		if (!problem) debugOutMenuItem.setEnabled(false);
	}

	public boolean gameLoaded() { return game == null ? false : true; }

	public void setDummyEditor() {
		setEditor(dummyEditor, getResourceMap().getString("Application.title"));
	}

	public void setDefaultEditor() {
		setEditor(new BinaryEditor(this), getResourceMap().getString("Application.title"));
	}

	public void setEditor(
		Editor<? extends PointerArray> newEditor, String title
	) {
		JFrame frame = app.getMainFrame();

		// Check if a Process is running.
		if (this.worker != null) {
			CommonDialogs.showBusyDialog(frame);
			return;
		}

		// Silently ignore a request to re-set the panel.
		if (currentEditor == newEditor) {
			return;
		}

		// Save anything that was changed by the current editor.
		// Camtech: Return if the user changed his/her mind, or if an error occurred.
		if (!currentEditor.teardown()) { return; }
		// Bring up the new editor.
		newEditor.setup(game);

		// Avoid automatically resizing the window when clearing the editor.
		Dimension d = frame.getSize();
		frame.setMinimumSize(null);
		frame.setTitle(title);
		setComponent(newEditor);
		frame.pack();

		// Set size constraints.
		// Hextator sez: This is no longer conditional due to the "dummy"
		// editor being replaced with an actual editor.
		//if (newEditor != dummyEditor) {
			frame.setMinimumSize(frame.getSize());
		//}
		frame.setSize(d);
		//frame.invalidate();
		//frame.repaint();
		// This seems to do the same thing
		frame.validate();
		currentEditor = newEditor;
	}

	public void process(
		boolean showDialogs, Process worker
	) {
		JFrame frame = app.getMainFrame();
		if (this.worker != null)
		{
			CommonDialogs.showBusyDialog(frame);
			return;
		}

		showDialogWhenComplete = showDialogs;

		if (showDialogs)
		{
			int option = CommonDialogs.showDelayWarningDialog();
			if (option == JOptionPane.NO_OPTION) { return; }
		}

		this.worker = worker;
		worker.run(this);
	}

	@Override
	public void propertyChange(PropertyChangeEvent event) {
		// This should be impossible
		if (worker == null) { throw new RuntimeException(); }

		String name = event.getPropertyName();
		if (name.equals("description")) {
			statusMessageLabel.setText((String)(event.getNewValue()));
		} else if (name.equals("state")) {
			StateValue value = (StateValue)(event.getNewValue());
			if (value.equals(StateValue.STARTED)) {
				progressBar.setVisible(true);
				busyIconTimer.start();
			} else if (value.equals(StateValue.DONE)) {
				// Clean up the process.
				progressBar.setValue(0);
				busyIconTimer.stop();
				statusAnimationLabel.setIcon(idleIcon);
				progressBar.setVisible(false);
				statusMessageLabel.setText(null);
				// Now it's safe to extract the final result and do something with it.
				worker.finish();
				worker = null;
				if (showDialogWhenComplete) {
					CommonDialogs.showOperationCompleteDialog();
				}
			}
		} else if (name.equals("progress")) {
			int progress = ((Integer)(event.getNewValue())).intValue();
			if (!progressBar.isIndeterminate()) {
				progressBar.setValue(progress);
			}
		} else if (name.equals("indeterminate")) {
			progressBar.setIndeterminate((Boolean)(event.getNewValue()));
		}
	}

	// Helpers for initialization.

	private JMenu createMenu(
		ResourceMap resourceMap,
		String name, char mnemonic,
		JMenuItem[] items
	) {
		JMenu menu = new JMenu();
		menu.setMnemonic(mnemonic);
		menu.setText(resourceMap.getString(name + ".text"));
		menu.setName(name);

		for (JMenuItem item: items) {
			menu.add(item);
		}

		return menu;
	}

	private JMenuItem createMenuItem(
		ActionMap actionMap, ResourceMap resourceMap,
		String name, KeyStroke accelerator,
		Integer mnemonic, boolean enabled
	) {
		JMenuItem item = new JMenuItem();
		item.setAction(actionMap.get(name + "Execute"));
		if (accelerator != null) { item.setAccelerator(accelerator); }
		if (mnemonic != null) { item.setMnemonic(mnemonic); }
		item.setText(resourceMap.getString(name + ".text"));
		item.setToolTipText(resourceMap.getString(name + ".toolTipText"));
		item.setName(name);
		item.setEnabled(enabled);
		return item;
	}

	private KeyStroke controlKeyStroke(int key) {
		return KeyStroke.getKeyStroke(key, InputEvent.CTRL_MASK);
	}

	private KeyStroke plainKeyStroke(int key) {
		return KeyStroke.getKeyStroke(key, 0);
	}

	private void initComponents() {
		ActionMap actionMap = Application.getInstance(App.class).getContext().getActionMap(View.class, this);
		ResourceMap resourceMap = getResourceMap();

		openMenuItem = createMenuItem(actionMap, resourceMap, "openMenuItem", controlKeyStroke(KeyEvent.VK_O), KeyEvent.VK_O, true);
		saveMenuItem = createMenuItem(actionMap, resourceMap, "saveMenuItem", controlKeyStroke(KeyEvent.VK_S), null, false);
		saveAsMenuItem = createMenuItem(actionMap, resourceMap, "saveAsMenuItem", null, KeyEvent.VK_S, false);
		saveCopyMenuItem = createMenuItem(actionMap, resourceMap, "saveCopyMenuItem", null, null, false);
		closeMenuItem = createMenuItem(actionMap, resourceMap, "closeMenuItem", controlKeyStroke(KeyEvent.VK_W), KeyEvent.VK_C, false);
		exitMenuItem = createMenuItem(actionMap, resourceMap, "exitMenuItem", plainKeyStroke(KeyEvent.VK_ESCAPE), KeyEvent.VK_X, true);

		classAnimationCreatorMenuItem = createMenuItem(actionMap, resourceMap, "classAnimationCreatorMenuItem", null, KeyEvent.VK_A, false);
		classAnimationManagerMenuItem = createMenuItem(actionMap, resourceMap, "classAnimationManagerMenuItem", null, KeyEvent.VK_M, false);
		portraitEditorMenuItem = createMenuItem(actionMap, resourceMap, "portraitEditorMenuItem", null, KeyEvent.VK_P, false);
		spellAnimationInserterMenuItem = createMenuItem(actionMap, resourceMap, "spellAnimationInserterMenuItem", null, KeyEvent.VK_S, false);
		textEditorMenuItem = createMenuItem(actionMap, resourceMap, "textEditorMenuItem", null, KeyEvent.VK_T, false);

		aboutMenuItem = new JMenuItem();
		aboutMenuItem.setAction(actionMap.get("showAboutBox"));
		aboutMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F1, InputEvent.SHIFT_MASK));
		aboutMenuItem.setName("aboutMenuItem");
		debugOutMenuItem = createMenuItem(actionMap, resourceMap, "debugOutMenuItem", controlKeyStroke(KeyEvent.VK_D), KeyEvent.VK_D, true);

		JMenuBar menuBar = new JMenuBar();
		menuBar.setName("menuBar");
		for (JMenu menu: new JMenu[] {
			createMenu(resourceMap, "fileMenu", 'f', new JMenuItem[] { openMenuItem, saveMenuItem, saveAsMenuItem, saveCopyMenuItem, closeMenuItem, exitMenuItem }),
			createMenu(resourceMap, "toolsMenu", 't', new JMenuItem[] { classAnimationCreatorMenuItem, classAnimationManagerMenuItem, portraitEditorMenuItem, spellAnimationInserterMenuItem, textEditorMenuItem }),
			createMenu(resourceMap, "helpMenu", 'h', new JMenuItem[] { aboutMenuItem, debugOutMenuItem })
		}) {
			menuBar.add(menu);
		}

		JPanel statusPanel = new JPanel();
		statusPanel.setName("statusPanel");

		JSeparator statusPanelSeparator = new JSeparator();
		statusPanelSeparator.setName("statusPanelSeparator");

		statusMessageLabel.setName("statusMessageLabel");

		statusAnimationLabel.setHorizontalAlignment(SwingConstants.LEFT);
		statusAnimationLabel.setName("statusAnimationLabel");

		progressBar.setName("progressBar");

		GroupLayout statusPanelLayout = new GroupLayout(statusPanel);
		statusPanel.setLayout(statusPanelLayout);
		statusPanelLayout.setHorizontalGroup(
			statusPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
			.addComponent(statusPanelSeparator, GroupLayout.DEFAULT_SIZE, 400, Short.MAX_VALUE)
			.addGroup(statusPanelLayout.createSequentialGroup()
				.addContainerGap()
				.addComponent(statusMessageLabel)
				.addPreferredGap(LayoutStyle.ComponentPlacement.RELATED, 230, Short.MAX_VALUE)
				.addComponent(progressBar, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
				.addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
				.addComponent(statusAnimationLabel)
				.addContainerGap())
		);
		statusPanelLayout.setVerticalGroup(
			statusPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
			.addGroup(statusPanelLayout.createSequentialGroup()
				.addComponent(statusPanelSeparator, GroupLayout.PREFERRED_SIZE, 2, GroupLayout.PREFERRED_SIZE)
				.addPreferredGap(LayoutStyle.ComponentPlacement.RELATED, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
				.addGroup(statusPanelLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
					.addComponent(statusMessageLabel)
					.addComponent(statusAnimationLabel)
					.addComponent(progressBar, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
				.addGap(3, 3, 3))
		);

		setComponent(dummyEditor);
		setMenuBar(menuBar);
		setStatusBar(statusPanel);
	}
}
