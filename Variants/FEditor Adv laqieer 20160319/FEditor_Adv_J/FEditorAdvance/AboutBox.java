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
 *  <Description> This class is the dialog displayed by the application
 *  when the user requests more information from the Help menu item
 */

package FEditorAdvance;

import org.jdesktop.application.Action;
import org.jdesktop.application.Application;
import org.jdesktop.application.ResourceMap;
import javax.swing.JLabel;
import javax.swing.JButton;
import javax.swing.GroupLayout;
import javax.swing.LayoutStyle.ComponentPlacement;

public class AboutBox extends javax.swing.JDialog {

	public AboutBox(java.awt.Frame parent) {
		super(parent);
		initComponents();
		getRootPane().setDefaultButton(closeButton);
	}

	@Action
	public void closeAboutBox() {
		setVisible(false);
	}

	private void embolden(JLabel label, int sizeDelta) {
		java.awt.Font font = label.getFont();
		label.setFont(
			font.deriveFont(
				font.getStyle() | java.awt.Font.BOLD, font.getSize() + sizeDelta
			)
		);
	}

	private JLabel createLabel(ResourceMap resourceMap, String textKey, String name) {
		JLabel label = new JLabel();
		label.setText(resourceMap.getString(textKey));
		label.setName(name);
		return label;
	}

	/** This method is called from within the constructor to
	 * initialize the form.
	 */
	private void initComponents() {
		ResourceMap resourceMap = Application.getInstance(App.class).getContext().getResourceMap(AboutBox.class);

		closeButton = new JButton();

		// Hextator: FINALLY got this to be different from the title
		// used for the main GUI. I had been wanting to.
		JLabel appTitleLabel = createLabel(resourceMap, "Application.fullTitle", "appTitleLabel");
		JLabel versionLabel = createLabel(resourceMap, "versionLabel.text", "versionLabel");
		JLabel appVersionLabel = createLabel(resourceMap, "Application.version", "appVersionLabel");
		JLabel vendorLabel = createLabel(resourceMap, "vendorLabel.text", "vendorLabel");
		JLabel appVendorLabel = createLabel(resourceMap, "Application.vendor", "appVendorLabel");
		JLabel appDescLabel = createLabel(resourceMap, "appDescLabel.text", "appDescLabel");
		JLabel appDescLabel1 = createLabel(resourceMap, "appDescLabel1.text", "appDescLabel1");

		embolden(versionLabel, 0);
		embolden(vendorLabel, 0);
		embolden(appTitleLabel, 4);
		appTitleLabel.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
		appTitleLabel.setAlignmentX(0.5F);

		setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
		setTitle(resourceMap.getString("title"));
		setModal(true);
		setName("aboutBox");
		setResizable(false);

		javax.swing.ActionMap actionMap = Application.getInstance(App.class).getContext().getActionMap(AboutBox.class, this);
		closeButton.setAction(actionMap.get("closeAboutBox"));
		closeButton.setName("closeButton");

		GroupLayout layout = new GroupLayout(getContentPane());
		getContentPane().setLayout(layout);
		layout.setHorizontalGroup(
			layout.createParallelGroup(GroupLayout.Alignment.LEADING)
			.addGroup(layout.createSequentialGroup()
				.addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
					.addGroup(layout.createSequentialGroup()
						.addContainerGap()
						.addGroup(layout.createParallelGroup(GroupLayout.Alignment.CENTER)
							.addComponent(appDescLabel)
							.addComponent(appDescLabel1)
							.addComponent(appTitleLabel)
							.addComponent(closeButton)))
					.addGroup(layout.createSequentialGroup()
						.addGap(10, 10, 10)
						.addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
							.addComponent(versionLabel)
							.addComponent(vendorLabel))
						.addPreferredGap(ComponentPlacement.RELATED)
						.addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
							.addComponent(appVersionLabel)
							.addComponent(appVendorLabel))))
				.addContainerGap(17, Short.MAX_VALUE))
		);
		layout.setVerticalGroup(
			layout.createParallelGroup(GroupLayout.Alignment.LEADING)
			.addGroup(layout.createSequentialGroup()
				.addContainerGap()
				.addComponent(appTitleLabel)
				.addGap(11, 11, 11)
				.addComponent(appDescLabel)
				.addGap(3, 3, 3)
				.addComponent(appDescLabel1)
				.addPreferredGap(ComponentPlacement.UNRELATED)
				.addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
					.addGroup(layout.createSequentialGroup()
						.addComponent(versionLabel)
						.addPreferredGap(ComponentPlacement.RELATED)
						.addComponent(vendorLabel))
					.addGroup(layout.createSequentialGroup()
						.addComponent(appVersionLabel)
						.addPreferredGap(ComponentPlacement.RELATED)
						.addComponent(appVendorLabel)))
				.addPreferredGap(ComponentPlacement.RELATED, 20, Short.MAX_VALUE)
				.addComponent(closeButton)
				.addContainerGap())
		);

		pack();
	}
    
	private JButton closeButton;
}
