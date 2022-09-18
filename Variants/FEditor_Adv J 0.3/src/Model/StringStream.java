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
 *  <Description> Simple interface for turning stream input into a String
 */

package Model;

import java.io.IOException;
import java.io.OutputStream;
import java.util.LinkedList;

public class StringStream extends OutputStream {
	private StringBuffer output = new StringBuffer("");
	private LinkedList<Listener> listeners = new LinkedList<Listener>();

	public String getOutput() {
		return output.toString();
	}

	private void fireUpdates() {
		for (Listener curr: listeners)
			curr.onUpdate();
	}

	@Override
	public void write(int b) throws IOException {
		output.append((char)b);
		fireUpdates();
	}

	public StringBuffer flushStream() {
		StringBuffer outputCopy = output;
		output = new StringBuffer("");
		fireUpdates();
		return outputCopy;
	}

	public void addListener(Listener toAdd) {
		listeners.add(toAdd);
	}

	public void removeListener(Listener toRemove) {
		listeners.remove(toRemove);
	}

	public void removeListener(int toRemove) {
		listeners.remove(toRemove);
	}

	public void removeAllListeners() {
		listeners = new LinkedList<Listener>();
	}

	public Listener[] getListeners() {
		Listener[] outputArray = new Listener[listeners.size()];
		int i = 0;
		for (Listener curr: listeners)
			outputArray[i++] = curr;
		return outputArray;
	}
}
