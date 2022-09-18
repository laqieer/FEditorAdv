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
 *  <Description> A wrapper for the common occurence of a hexadecimal spinner
 *  labeled with a JLabel
 */

package Controls;

import javax.swing.event.ChangeListener;
import javax.swing.event.ChangeEvent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;

public class LabelledHexSpinner extends JPanel {
	private SpinnerNumberModel model;
	private HexNumberEditor editor;
	private JSpinner spinner;
	private JLabel label;

	public LabelledHexSpinner(
		final IntValueListener owner, String title, int digits
	) {
		super();

		label = new JLabel(title);
		int max = (int)(((long)1 << 4 * digits) - 1);
		int min = 0;
		if (max == -1) {
			max = Integer.MAX_VALUE;
			min = Integer.MIN_VALUE;
		}
		model = new SpinnerNumberModel(0, min, max, 1);
		spinner = new JSpinner(model);
		editor = new HexNumberEditor(spinner, digits);
		spinner.setEditor(editor);

		if (owner != null) {
			spinner.addChangeListener(new ChangeListener() {
				public void stateChanged(ChangeEvent e) {
					owner.newValue(getValue());
				}				
			});
		}

		add(label);
		add(spinner);
	}

	public LabelledHexSpinner(
		final IntValueListener owner, String title, int digits,
		int min, int value, int max
	) {
		this(owner, title, digits);
		init(min, value, max);
	}

	public void init(int min, int value, int max) {
		if (value < min || value > max) { throw new IllegalArgumentException(); }

		// Don't fire any property changes.
		ChangeListener[] listeners = spinner.getChangeListeners();
		for (ChangeListener listener: listeners) {
			spinner.removeChangeListener(listener);
		}
		model.setMinimum(new Integer(min));
		model.setMaximum(new Integer(max));
		for (ChangeListener listener: listeners) {
			spinner.addChangeListener(listener);
		}
		model.setValue(new Integer(value));
	}

	public int getMin() {
		return ((Integer)(model.getMinimum())).intValue();
	}

	public void setMin(int value) {
		// Why doesn't this happen automatically?
		if (getValue() < value) { setValue(value); }
		model.setMinimum(new Integer(value));
	}

	public void changeMin(int amount) {
		setMin(getMin() + amount);
	}

	public int getValue() {
		return ((Integer)(model.getValue())).intValue();
	}

	public void setValue(int value) {
		model.setValue(new Integer(value));
	}

	public void changeValue(int amount) {
		setValue(getValue() + amount);
	}

	public int getMax() {
		return ((Integer)(model.getMaximum())).intValue();
	}

	public void setMax(int value) {
		// Why doesn't this happen automatically?
		if (getValue() > value) { setValue(value); }
		model.setMaximum(new Integer(value));
	}

	public void changeMax(int amount) {
		setMax(getMax() + amount);
	}

	public void changeStep(int newStep) {
		model.setStepSize(newStep);
	}
}
