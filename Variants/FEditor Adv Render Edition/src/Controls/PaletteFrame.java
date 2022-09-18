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
 *  <Description> This class provides a dialog for inserting custom spell
 *  animations
 */

package Controls;

import java.awt.image.BufferedImage;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ChangeEvent;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
//import javax.swing.JSlider;
import Graphics.Palette;

public class PaletteFrame extends Box {
	class ColorChooser extends Box {
		private LabelledHexSpinner[] spinners = new LabelledHexSpinner[3];
		private int index = -1;
		private final ChangeListener listener;
		private ChangeEvent event;

		public ColorChooser(ChangeListener listener) {
			super(BoxLayout.PAGE_AXIS);
			this.listener = listener;
			JPanel spinnerPanel = new JPanel();
			for (int i = 0; i < 3; ++i) {
				//JSlider slider = new JSlider(0, 31);
				//slider.setSnapToTicks(true);
				//slider.setMinorTickSpacing(1);
				/*
				slider.addChangeListener(new ChangeListener() {
					public void stateChanged(ChangeEvent e) {
						event = e;
						update();
					}
				});
				*/
				//add(slider);
				//spinners[i] = slider;
				String labelString = "";
				switch (i) {
					case 0: labelString = "R:"; break;
					case 1: labelString = "G:"; break;
					case 2: labelString = "B:"; break;
				}
				LabelledHexSpinner spinner = new LabelledHexSpinner(
					new IntValueListener() {
						public void newValue(int value) {
							update();
						}
					}, labelString, 2,
					0, 0, 31
				);
				spinnerPanel.add(spinner);
				spinners[i] = spinner;
			}
			add(spinnerPanel);
		}

		public void setIndex(int index) {
			if (this.index == index) { return; }
			if (this.index != -1) {
				ColorButton oldButton = buttons[this.index];
				oldButton.setColor(oldButton.getColor(), false);
			}
			this.index = index;
			synchronized (this) {
				//System.out.println(Util.verboseReport("begin setindex"));
				int argb = palette.getARGB(this.index);
				spinners[2].setValue((argb & 0xF8) >> 3);
				spinners[1].setValue((argb & 0xF800) >> 11);
				spinners[0].setValue((argb & 0xF80000) >> 19);
				//System.out.println(Util.verboseReport("end setindex"));
			}
		}

		private synchronized void update() {
			short rgb = (short)(
				spinners[0].getValue() |
				spinners[1].getValue() << 5 |
				spinners[2].getValue() << 10
			);
			/*
			System.out.println(Util.verboseReport(String.format(
				"Updating color to 0x%04X", rgb & 0xFFFF
			)));
			*/
			palette.setIndex(index, rgb);
			int argb = palette.getARGB(index);
			buttons[index].setColor(argb, true);
			if (listener != null) { listener.stateChanged(event); }
		}
	}

	class ColorButton extends JButton {
		private static final int SIZE = 16;
		private BufferedImage image = new BufferedImage(
			SIZE, SIZE, BufferedImage.TYPE_INT_ARGB
		);
		private int color;

		public void setColor(int argb, boolean selected) {
			int datasize = SIZE * SIZE;
			int[] data = new int[datasize];
			for (int i = 0; i < datasize; ++i) {
				data[i] = argb;
				int x = i % SIZE, y = i / SIZE;
				if (((argb >> 24) & 0xFF) == 0) {
					// transparent: draw an X through the icon.
					int difference = x - y;
					int sum = x + y - SIZE;
					if ((difference * difference) == 0 || (sum * sum) == 0) {
						data[i] = 0;
					} else if ((difference * difference) == 1 || (sum * sum) == 1) {
						data[i] = 0xFFFFFF;
					}
				}
				if (selected) {
					// Draw a border around the icon 
					if (x == 0 || x == SIZE - 1 || y == 0 || y == SIZE - 1) {
						data[i] = 0;
					} else if (x == 1 || x == SIZE - 2 || y == 1 || y == SIZE - 2) {
						data[i] = 0xFFFFFF;
					}
				}
				data[i] |= 0xFF000000;
			}
			image.setRGB(0, 0, SIZE, SIZE, data, 0, SIZE);
			setIcon(new ImageIcon(image));
			color = argb;
		}

		public int getColor() {
			return color;
		}
	}

	private Palette palette;
	private ColorButton[] buttons = new ColorButton[16];

	private ColorChooser chooser;

	public PaletteFrame(ChangeListener listener) {
		super(BoxLayout.PAGE_AXIS);
		chooser = new ColorChooser(listener);
		Box[] rows = new Box[2];
		for (int i = 0; i < 2; ++i) {
			rows[i] = new Box(BoxLayout.LINE_AXIS);
			add(rows[i]);
		}
		for (int i = 0; i < 16; ++i) {
			final int index = i;
			final ColorButton button = new ColorButton();
			buttons[i] = button;
			button.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) { chooser.setIndex(index); }
			});
			rows[i / 8].add(buttons[i], 0); // reverse the order in each line
		}
		add(chooser);
	}

	public void setPalette(Palette palette) {
		this.palette = palette;
		for (int i = 0; i < 16; ++i) {
			buttons[i].setColor(palette.getARGB(i), i == 0);
		}
		chooser.setIndex(0);
	}

	public Palette getPalette() {
		return palette.copy();
	}

	@SuppressWarnings("static-access")
	public static void main(String[] args) {
		short[] test = new short[16];
		for (int i = 0; i < 16; ++i) { test[i] = (short)(i * 0x842); }
		Palette p = new Palette(test);
		JFrame f = new JFrame();
		PaletteFrame pf = new PaletteFrame(null);
		pf.setPalette(p);
		f.add(pf);
		f.setDefaultCloseOperation(f.EXIT_ON_CLOSE);
		f.pack();
		f.setVisible(true);
		f.setMinimumSize(f.getSize());
		f.setMaximumSize(f.getSize());
	}
}

