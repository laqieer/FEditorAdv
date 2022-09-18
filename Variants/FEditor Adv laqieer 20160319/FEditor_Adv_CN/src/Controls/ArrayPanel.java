/*
 *  FE Editor - GBA Fire Emblem (U) ROM editor
 *
 *  Copyright (C) 2008-2011 Hextator,
 *  hextator (AIM) hextator@gmail.com (MSN)
 *
 *  Major thanks to Zahlman (AIM/MSN: zahlman@gmail.com) for optimization,
 *  organization and modularity improvements.
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
 *  <Description> A panel used to control the position (index) and size
 *  (max index = size - 1) of a pointer array.
 */

package Controls;

import javax.swing.JPanel;
import Model.PointerArray;
import FEditorAdvance.Editor;

public class ArrayPanel<A extends PointerArray> extends JPanel {
	private LabelledHexSpinner index, maxIndex;
	private A array;
	private Editor<A> editor;
	private int first_edit_index, max_edit_index, offset;
	private IntValueListener indexListener;

	public ArrayPanel(Editor<A> editor, int first_edit_index, int max_edit_index, int offset) {
		this.editor = editor;
		this.first_edit_index = first_edit_index;
		this.max_edit_index = max_edit_index;
		this.offset = offset;

		// Calculate how many hex digits are needed to display the biggest value
		// that will ever appear on 'maxIndex'.
		int digits = Integer.toHexString(max_edit_index).length();
		indexListener =
			new IntValueListener() {
				public void newValue(int value) { refresh(value); }
			};
		index = new LabelledHexSpinner(
			indexListener , "输入索引:", digits
		);
		maxIndex = new LabelledHexSpinner(
			new IntValueListener() {
				public void newValue(int value) { setMax(value); }
			}, "最大索引:", digits
		);

		add(index);
		add(maxIndex);
	}

	public void setArray(A array) {
		this.array = array;
		if (array == null) { return; }
		int size = array.getCurrentSize();
		index.init(first_edit_index, first_edit_index, size + offset - 1);
		maxIndex.init(first_edit_index, size + offset - 1, max_edit_index);
		refresh(first_edit_index);
	}

	public void setIndex(int value) {
		if (value == index.getValue()) {
			// Then a setValue() call wouldn't trigger the listener, so we
			// have to refresh manually.
			refresh(value);
		} else {
			// Otherwise, we want the value on the spinner to change too, so we
			// change the spinner and let the listener invoke updateIndex().
			index.setValue(value);
		}
	}

	public int getIndex() { return index.getValue(); }

	private void refresh(int value) {
		// In some cases, the array will already be at this position, but it's
		// cheaper and simpler to set the position blindly than to check.
		array.moveTo(value - offset);
		editor.refresh();
	}

	private void setMax(int max) {
		if (max < first_edit_index) { throw new IllegalArgumentException(); }
		// TODO: Add confirmation if the resize will erase existing data.
		array.resize(max - offset + 1);
		index.setMax(max);
	}
}
