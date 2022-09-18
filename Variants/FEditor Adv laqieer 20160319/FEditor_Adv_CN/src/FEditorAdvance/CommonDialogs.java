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
 *  <Description> This class simplifies the process of constructing dialogs
 *  that are commonly used and would have unnecessarily redundant display
 *  arguments otherwise
 */

package FEditorAdvance;

import java.awt.Component;
import java.io.File;
import javax.swing.JOptionPane;
import Model.Util;

public class CommonDialogs {
	public static int showDelayWarningDialog() {
		return JOptionPane.showConfirmDialog(
			null,
			"这个过程会消耗很长时间\n" +
			"依然继续?",
			"输入",
			JOptionPane.YES_NO_OPTION
		);
	}

	public static void showOperationCompleteDialog() {
		JOptionPane.showMessageDialog(
			null,
			"操作完成",
			"成功",
			JOptionPane.INFORMATION_MESSAGE
		);
	}

	public static boolean showSaveDialog() {
		int choice = JOptionPane.showConfirmDialog(
			null,
			"保存?",
			"输入",
			JOptionPane.YES_NO_OPTION
		);

		return choice == JOptionPane.YES_OPTION;
	}

	public static void showBusyDialog(Component parent) {
		JOptionPane.showMessageDialog(
			parent,
			"一次不可以同时使用多个编辑器\n" +
			"不要在一个编辑器活动时操作文件\n" +
			" ",
			"错误",
			JOptionPane.ERROR_MESSAGE
		);
	}

	public static void showStreamErrorDialog() {
		javax.swing.JOptionPane.showMessageDialog (
			null,
			"一个或多个文件流处理过程中出错",
			"错误",
			javax.swing.JOptionPane.ERROR_MESSAGE
		);
		throw new RuntimeException();
	}

	public static void showGenericMessageDialog(
		String message, String title,
		boolean informationMessage
	) {
		javax.swing.JOptionPane.showMessageDialog(
			null,
			message,
			title,
			informationMessage
			? javax.swing.JOptionPane.INFORMATION_MESSAGE
			: javax.swing.JOptionPane.PLAIN_MESSAGE
		);	
	}

	public static void showGenericErrorDialog(String message) {
		javax.swing.JTextArea errorTextArea =
			new javax.swing.JTextArea();
		errorTextArea.setText(message + "\n");
		errorTextArea.setRows(16);
		errorTextArea.setColumns(48);
		errorTextArea.setEditable(false);
		javax.swing.JScrollPane errorScrollPane =
			new javax.swing.JScrollPane(errorTextArea);
		JOptionPane.showMessageDialog(
			null, errorScrollPane, "错误",
			JOptionPane.ERROR_MESSAGE
		);
	}

	public static /*<E extends Exception>*/ void showCatchErrorDialog(
		Exception e //E e
	) {
		if (e == null) return;
		showGenericErrorDialog(Util.verboseException(e));
	}

	private static File fileHelper(String title, int mode) {
		java.awt.FileDialog chooser = new java.awt.FileDialog(
			new java.awt.Frame(), title, mode
		);
		chooser.setVisible(true);
		chooser.setLocationRelativeTo(null);

		String directory = chooser.getDirectory();
		String file = chooser.getFile();

		if (directory == null || file == null) { return null; }
		return new File(directory + file);
	}

	public static File showOpenFileDialog(String what) {
		return fileHelper(
			"选择 " + what + " 打开",
			java.awt.FileDialog.LOAD
		);
	}

	public static File showSaveFileDialog(String what) {
		return fileHelper(
			"选择保存路径 " + what,
			java.awt.FileDialog.SAVE
		);
	}

	public static String showSaveAsDialog() {
		return fileHelper(
			"保存",
			java.awt.FileDialog.SAVE
		).getPath();
	}

	// Get an integer in [min, max] by repeatedly prompting.
	public static Integer promptForInteger(String prompt, int min, int max) {
		while (true) {
			String response = JOptionPane.showInputDialog(null, prompt, 0);
			//System.out.println(response);
			if (response == null) { return null; }

			try {
				int result = Util.parseInt(response);
				if (result >= min && result <= max) { return result; }
			} catch (Exception e) {}
		}
	}
}
