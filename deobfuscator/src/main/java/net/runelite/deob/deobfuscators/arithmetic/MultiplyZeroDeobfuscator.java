/*
 * Copyright (c) 2016, Adam <Adam@sigterm.info>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 * 3. All advertising materials mentioning features or use of this software
 *    must display the following acknowledgement:
 *    This product includes software developed by Adam <Adam@sigterm.info>
 * 4. Neither the name of the Adam <Adam@sigterm.info> nor the
 *    names of its contributors may be used to endorse or promote products
 *    derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY Adam <Adam@sigterm.info> ''AS IS'' AND ANY
 * EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL Adam <Adam@sigterm.info> BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package net.runelite.deob.deobfuscators.arithmetic;

import java.util.List;
import net.runelite.asm.ClassGroup;
import net.runelite.deob.Deobfuscator;
import net.runelite.asm.attributes.code.Instruction;
import net.runelite.asm.attributes.code.Instructions;
import net.runelite.asm.attributes.code.instruction.types.PushConstantInstruction;
import net.runelite.asm.attributes.code.instructions.IMul;
import net.runelite.asm.attributes.code.instructions.LDC2_W;
import net.runelite.asm.attributes.code.instructions.LDC_W;
import net.runelite.asm.attributes.code.instructions.LMul;
import net.runelite.asm.execution.Execution;
import net.runelite.asm.execution.InstructionContext;
import net.runelite.asm.execution.MethodContext;
import net.runelite.asm.execution.StackContext;

public class MultiplyZeroDeobfuscator implements Deobfuscator
{
	private int count;

	private void visit(MethodContext mctx)
	{
		for (InstructionContext ictx : mctx.getInstructionContexts())
		{
			Instruction instruction = ictx.getInstruction();
			Instructions ins = instruction.getInstructions();
			if (ins == null)
			{
				continue;
			}

			if (!(instruction instanceof IMul) && !(instruction instanceof LMul))
			{
				continue;
			}

			List<Instruction> ilist = ins.getInstructions();

			StackContext one = ictx.getPops().get(0);
			StackContext two = ictx.getPops().get(1);

			Instruction ione = one.getPushed().getInstruction(),
				itwo = two.getPushed().getInstruction();

			boolean remove = false;
			if (ione instanceof PushConstantInstruction)
			{
				PushConstantInstruction pci = (PushConstantInstruction) ione;
				Number value = (Number) pci.getConstant().getObject();

				if (DMath.equals(value, 0))
				{
					remove = true;
				}
			}
			if (itwo instanceof PushConstantInstruction)
			{
				PushConstantInstruction pci = (PushConstantInstruction) itwo;
				Number value = (Number) pci.getConstant().getObject();

				if (DMath.equals(value, 0))
				{
					remove = true;
				}
			}

			if (remove == false)
			{
				continue;
			}

			if (!ilist.contains(instruction))
			{
				continue; // already done
			}
			if (!MultiplicationDeobfuscator.isOnlyPath(ictx, null))
			{
				continue;
			}

			// remove both, remove imul, push 0
			ictx.removeStack(1);
			ictx.removeStack(0);

			if (instruction instanceof IMul)
			{
				ins.replace(instruction, new LDC_W(ins, new net.runelite.asm.pool.Integer(0)));
			}
			else if (instruction instanceof LMul)
			{
				ins.replace(instruction, new LDC2_W(ins, 0L));
			}
			else
			{
				throw new IllegalStateException();
			}

			++count;

		}
	}
	
	@Override
	public void run(ClassGroup group)
	{
		Execution e = new Execution(group);
		e.addMethodContextVisitor(i -> visit(i));
		e.populateInitialMethods();
		e.run();
		
		System.out.println("Removed " + count + " 0 multiplications");
	}
}