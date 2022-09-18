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
 *  <Description> This class provides a container for interfaces that extend
 *  the JComponent class
 */

package FEditorAdvance;

import org.jdesktop.application.FrameView;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.SwingWorker;
import Model.Listener;

public class ModelessFrame extends FrameView {
	// TODO: Refactor this out into a model class
	public static class InstanceMonitor {
		private boolean problem = false;
		private boolean done = false;
		private Object output = null;

		public void problem() { problem = true; }

		public void done() { done = true; }

		public boolean hasProblem() { return problem; }

		public boolean isDone() { return done; }

		public Object getOutput() { return output; }

		public void setOutput(Object input) { output = input; }
	}

	private static App app = App.getApplication();

	private FEditorAdvance.ScrollableComponent center;

	public void pack() {
		if (center == null) return;

		int screenWidth = Toolkit.getDefaultToolkit().getScreenSize().width;
		int screenHeight = Toolkit.getDefaultToolkit().getScreenSize().height;
		int threeFourthsWidth = (screenWidth * 3)/4;
		int threeFourthsHeight = (screenHeight * 3)/4;
		JFrame frame = getFrame();
		frame.setTitle(center.getTitle());
		frame.setLocationRelativeTo(null);
		// Toss out old bounds; they will be updated appropriately
		frame.setMinimumSize(null);
		frame.setMaximumSize(null);
		frame.pack();
		int extraWidth = center.extraWidth();
		int extraHeight = center.extraHeight();
		int width = frame.getPreferredSize().width;
		int height = frame.getPreferredSize().height;
		width += extraWidth;
		height += extraHeight;
		if (height > threeFourthsHeight)
			height = threeFourthsHeight;
		if (width > threeFourthsWidth)
			width = threeFourthsWidth;
		frame.setSize(new Dimension(width, height));
		// Bound dimensions appropriately
		frame.setMinimumSize(frame.getSize());
		frame.setMaximumSize(new Dimension(
			screenWidth, frame.getSize().height
		));
		// Graphics need to be told to be updated by now due to
		// replacing the entire central component
		frame.repaint();
	}

	public static InstanceMonitor newInstance(
		final FEditorAdvance.ScrollableComponent center,
		final Component parent,
		final Listener onCloseAction
	) {
		final InstanceMonitor monitor = new InstanceMonitor();
		SwingWorker<Void, Void> thread =
			new SwingWorker<Void, Void>() {
			@Override
			protected Void doInBackground() throws Exception {
				try {
					monitor.setOutput(new ModelessFrame(
						center, parent, onCloseAction
					));
					monitor.done();
				} catch (Exception e) { monitor.problem(); }
				return null;
			}
		};
		thread.execute();
		return monitor;
	}

	private ModelessFrame(
		final FEditorAdvance.ScrollableComponent center,
		final Component parent,
		final Listener onCloseAction
	) {
		super(app);
		this.center = center;

		final ModelessFrame curr = this;
		final Listener closeAction;
		if (onCloseAction == null)
			closeAction = new Listener() {
				public void onUpdate() {}
			};
		else
			closeAction = onCloseAction;

		JFrame frame = getFrame();
		frame.addWindowListener(new WindowListener() {
			public void windowOpened(WindowEvent e) {}

			public void windowClosing(WindowEvent e) {
				closeAction.onUpdate();
				center.close();
				JFrame frame = curr.getFrame();
				frame.setVisible(false);
				frame.dispose();
			}

			public void windowClosed(WindowEvent e) {}

			public void windowIconified(WindowEvent e) {}

			public void windowDeiconified(WindowEvent e) {}

			public void windowActivated(WindowEvent e) {}

			public void windowDeactivated(WindowEvent e) {}
		});

		setComponent((JComponent)center);
		pack();

		frame.setVisible(true);
		frame.setLocationRelativeTo(parent);
	}

	public void appear(Component parent) {
		JFrame frame = getFrame();
		frame.setVisible(true);
		frame.setLocationRelativeTo(parent);
	}
}
