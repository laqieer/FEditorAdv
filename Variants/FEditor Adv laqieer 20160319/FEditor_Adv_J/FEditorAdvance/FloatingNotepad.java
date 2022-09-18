/*
 *  FE Editor - GBA Fire Emblem (U) ROM editor
 *
 *  Copyright (C) 2008-2011 Hextator,
 *  hextator (AIM) hextator@gmail.com (MSN)
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
 *  <Description> This class provides an interface for dumping/editing text
 */

package FEditorAdvance;

import Model.Listener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeListener;
import javax.swing.Action;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.KeyStroke;
import Model.StringStream;

public class FloatingNotepad extends Box implements ScrollableComponent {
	private String title;

	private StringStream inputStream = null;
	private StringBuffer flushed = new StringBuffer("");
	private JTextArea textArea;
	private JScrollPane scrollPane;
	private JButton clearButton;
	private JPanel buttonPanel;
	private JPanel topPanel = new JPanel();
	private Listener listener;

	private void update() {
		textArea.setText(inputStream.getOutput());
		textArea.setCaretPosition(textArea.getDocument().getLength());
	}

	public FloatingNotepad(
		String title, StringStream input,
		boolean editable
	) {
		super(BoxLayout.PAGE_AXIS);
		this.title = title;

		topPanel.setLayout(new BoxLayout(topPanel, BoxLayout.PAGE_AXIS));
		inputStream = input;
		textArea = new JTextArea(24, 48);
		if (!editable)
			textArea.setEditable(false);
		scrollPane = new JScrollPane(textArea);
		scrollPane.setVerticalScrollBarPolicy(
			JScrollPane.VERTICAL_SCROLLBAR_ALWAYS
		);
		topPanel.add(scrollPane);

		Action clearAction = new Action() {
			public Object getValue(String key) {
				return null;
			}

			public void putValue(String key, Object value) {}

			public void setEnabled(boolean b) {}

			public boolean isEnabled() {
				return inputStream != null;
			}

			public void addPropertyChangeListener(
				PropertyChangeListener listener
			) {}

			public void removePropertyChangeListener(
				PropertyChangeListener listener
			) {}

			public void actionPerformed(ActionEvent e) {
				flushPad();
			}
		};
		clearButton = new JButton("クリア");
		clearButton.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(
			KeyStroke.getKeyStroke("ctrl L"),
			"クリア"
		);
		clearButton.getActionMap().put("クリア", clearAction);
		clearButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				flushPad();
			}
		});
		buttonPanel = new JPanel();
		buttonPanel.add(clearButton);
		topPanel.add(buttonPanel);

		add(topPanel);
		update();

		listener = new Listener() {
			public void onUpdate() {
				try {
					update();
				} catch (Exception e) {}
			}
		};
		inputStream.addListener(listener);
	}

	public StringBuffer flushPad() {
		if (inputStream != null)
			flushed = inputStream.flushStream();
		return flushed;
	}

	public StringBuffer getFlushedData() {
		return flushed;
	}

	public void close() { inputStream.removeListener(listener); }

	public String getTitle() { return title; }

	public int extraWidth() {
		return scrollPane.getVerticalScrollBar().getWidth();
	}

	public int extraHeight() {
		return scrollPane.getHorizontalScrollBar().getHeight();
	}
}
