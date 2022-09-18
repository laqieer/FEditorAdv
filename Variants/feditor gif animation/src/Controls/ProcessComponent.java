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
 *  <Description> A class that encapsulates a stage of a "process" which takes
 *  a long time to execute and during which update() messages are sent
 *  periodically.
 *  A message is sent after each iteration of each ProcessComponent.
 */

package Controls;

abstract public class ProcessComponent {
	final int iterations;
	final int weight;
	final String description;

	public ProcessComponent(int iterations, int weight, String description) {
		this.iterations = iterations;
		this.weight = weight;
		this.description = description;
	}

	abstract protected boolean iterate(int i);

	protected void cleanup() {}
}
