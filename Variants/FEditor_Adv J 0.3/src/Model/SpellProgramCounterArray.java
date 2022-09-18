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
 *  <Description> This class provides an interface for updating/managing the
 *  spell animation program counter array
 */

package Model;

public class SpellProgramCounterArray extends PointerArray {
	private int dim_pc, no_dim_pc;

	public SpellProgramCounterArray(
		int dim_pc, int no_dim_pc,
		Game owner,
		ROM.Pointer handle, ROM.Pointer default_base,
		int default_size, int record_size
	) {
		super(owner, handle, default_base, default_size, record_size);

		// We keep this array at the maximum size until it's time to write back,
		// at which point we trim it to the same size as the spell animation array.

		// TODO: Consider letting the SPT size be (number of stock ROM animations)
		// + (SAT size), allowing up to 256 custom spells (since the stock spells
		// never use the SAT, as the SAT doesn't exist in the stock game). This
		// might be impossible due to SPT being indexed with a byte elsewhere?
		// Is it OK if the stock code indexes the SPT with a byte and the CSA patch
		// indexes it with a short?
		// Hextator: I'm not sure what you mean. 256 entries IS being accessed by
		// a byte. If you mean "there could be 65536 - 256 custom spells" then
		// I suppose that's a possibility, but not a priority.
		// Zahlman: A byte accesses 256 entries, but right now, entries are
		// assigned to stock game spells, which is a waste. I was proposing to
		// allow 256 custom spells, plus the 60-odd from the game, by having a
		// PC array with 256 + 60-odd entries and a SA array with 256 entries,
		// corresponding only to the animations that are custom.
		// But maybe it makes more sense to just let there be 256 total spells,
		// and save the unusable space in the SAT by offsetting.
		resize(maxSize());

		this.dim_pc = dim_pc;
		this.no_dim_pc = no_dim_pc;
	}

	public int getPC() { return readValue(0); }

	protected void moveToEndOfData(ROM.Pointer p, int index) {}

	protected byte[] prepare(byte[] newData, int index) { return newData; }

	protected boolean markNewPointer() { return false; }

	public void setPC(boolean dim) {
		writeValue(0, dim ? dim_pc : no_dim_pc);
		System.out.println("Writing done by " + Util.methodName());
	}

	@Override
	protected boolean isPointer(int index) { return false; }

	// Since index 0 is valid for this array, 0x100 entries are indexable.
	@Override
	public int maxSize() { return 0x100; }
}
