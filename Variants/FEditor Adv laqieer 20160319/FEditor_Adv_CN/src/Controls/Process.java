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
 *  <Description> SwingWorker<Void, Void> wrapper for better organization
 *  of multi-threaded code
 */

package Controls;

import java.beans.PropertyChangeListener;
import javax.swing.SwingWorker;

public class Process extends SwingWorker<Void, Void> {
	protected ProcessComponent[] components;
	private int totalProgress = 0;
	private Boolean cancel_state;

	public Process(boolean canCancel, ProcessComponent... components) {
		this.components = components;
		for (ProcessComponent component: components) {
			totalProgress += component.weight;
		}
		if (canCancel) { cancel_state = Boolean.FALSE; }
	}

	public final void run(PropertyChangeListener toUpdate) {
		addPropertyChangeListener(toUpdate);
		execute();
	}		

	public void finish() {}

	public final void cancel() {
		if (cancel_state == Boolean.FALSE) { cancel_state = Boolean.TRUE; }
	}

	protected final Void doInBackground() {
		//final int count = components.length;

		firePropertyChange("不定的", totalProgress != 0, totalProgress == 0);

		int progress = 0;

		for (ProcessComponent component: components) {
			firePropertyChange("说明", null, component.description);
			int iterations = component.iterations;
			int weight = component.weight;

			//boolean aborted = false;
			try {
				for (int i = 0; i < iterations; ++i) {
					if (cancel_state == Boolean.TRUE) { break; }
					if (component.iterate(i)) { break; }
					if (totalProgress != 0) {
						setProgress(
							100 * ((progress * iterations) + (i * weight)) /
							(iterations * totalProgress)
						);
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				component.cleanup();
			}

			progress += component.weight;
		}

		return null;
	}
};
