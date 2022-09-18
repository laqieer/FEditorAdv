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
 *  <Description> This class provides a dialog for inserting custom spell
 *  animations
 */

package FEditorAdvance;

import java.awt.event.ActionListener;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JPanel;
import Model.Game;
import Model.PointerArray;

public class Editor<T extends PointerArray> extends Box {
	// Base class for editor modules. It's possible that this should be an
	// interface instead, but for now this is the simplest thing.
	// This class provides some common setup and teardown logic for the various
	// editors.
	// The base class acts as a "dummy" instance.
	
	protected View view;
	protected T arrayHandle;

	protected JButton addButtonToPanel(
		JPanel buttonPanel, String label, ActionListener actionListener
	) {
		JButton button = new JButton(label);
		button.addActionListener(actionListener);
		buttonPanel.add(button);
		return button;
	}

	public Editor(View view) {
		super(BoxLayout.PAGE_AXIS);
		this.view = view;
	}

	// Called when the array index changes. Should be used to load new contents
	// into display components.
	public void refresh() {}

	// Called when the editor is invoked. Subclasses should override this to
	// extract the necessary array.
	public void setup(Game game) {}

	public boolean isSaved() {
		return (arrayHandle == null ? true : arrayHandle.isSaved());
	}

	// Called when the editor is put away.
	// Camtech: I changed this to return an int, since that was the only way
	// to stop the new editor from being loaded by View.
	// Hextator: Modified Cam's change to return a boolean of whether
	// to proceed with teardown
	public final boolean teardown() {
		if (!changesSaved()) {
			int toSave = javax.swing.JOptionPane.showConfirmDialog(
				null, "Apply unsaved changes?"
			);
			switch(toSave) {
				case javax.swing.JOptionPane.YES_OPTION:
					applyChanges();
				case javax.swing.JOptionPane.NO_OPTION:
					// Do nothing; default behaviour
					break;
				default:
					return false;
			}
		}
		cleanup();

		if (arrayHandle == null) { return true; } // dummy instance; nothing to do.
		// Write the array to the Game instance. If teardown() was called due to
		// a close/quit without saving request, this will write into the Game
		// instance, but then the Game is just discarded, so everything works out
		// as desired on disk.
		arrayHandle.save();
		// Hextator sez: Most overrides of "cleanup" will do what the
		// following line does...but the thing is, not only is this
		// redundant, but it shouldn't be necessary to begin with
		// due to how Java handles GC (I think?)
		// Doesn't hurt to leave it in, however
		arrayHandle = null; // allow for GC
		return true;
	}

	// A hook for editor-specific cleanup to deallocate resources.
	protected void cleanup() { }
	
	// Camtech: Added applyChanges and changesSaved methods

	public void applyChanges() { }

	public boolean changesSaved() { return true; }

	// Teardown self and go back to a dummy editor.
	public final void quit() {
		if (view.gameLoaded())
			view.setDefaultEditor();
		else
			view.setDummyEditor();
	}
}
