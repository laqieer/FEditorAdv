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
 *  <Description> This class provides the initialization of the
 *  application
 */

package FEditorAdvance;

import org.jdesktop.application.Application;
import org.jdesktop.application.SingleFrameApplication;
import java.io.PrintStream;
import Model.StringStream;

/**
 * The main class of the application.
 */
public class App extends SingleFrameApplication {
	private static StringStream redirectedSystemOut;

	private String initPath;

	/**
	 * At startup create and show the main frame of the application.
	 */
	@Override
	protected void startup() {
		// Hextator sez: This had to be removed from the View
		// constructor and placed here to allow the View to completely
		// load and get along with Swing, which seems to despite
		// loading an Editor within the constructor that is not empty
		View temp = new View(this);
		show(temp);
		temp.open(initPath);
	}

	@Override
	protected void initialize(String[] args) {
		if (args.length > 0) { initPath = args[0]; }
	}

	public static App getApplication() {
		return Application.getInstance(App.class);
	}

	public static StringStream getSysOutStream() {
		return redirectedSystemOut;
	}

	/**
	 * Main method launching the application.
	 */
	public static void main(String[] args) {
		redirectedSystemOut = new StringStream();
		PrintStream ps = new PrintStream(redirectedSystemOut);
		System.setOut(ps);
		launch(App.class, args);
	}
}
