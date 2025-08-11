package com.hitchhikerprod.dragonjars.exec.instructions;

import com.hitchhikerprod.dragonjars.exec.Address;
import com.hitchhikerprod.dragonjars.exec.Interpreter;

public class DrawModal implements Instruction {
    // x is a character address, y is a pixel address
    // bbox is x0,y0,x1,y1
    @Override
    public Address exec(Interpreter i) {
        final Address ip = i.getIP();
        final int x0 = i.readByte(ip.incr(1)) * 8; // ch adr -> pix adr
        final int y0 = i.readByte(ip.incr(2));
        final int x1 = i.readByte(ip.incr(3)) * 8;
        final int y1 = i.readByte(ip.incr(4));
        i.drawModal(x0, y0, x1, y1);

        // four immediates: 16 00 28 98 (combat window)
        // written as words to 0x253f/tmp
        // draw_string_313e (to empty buffer??)
        // if 0x253e/draw_borders > 0 {           <- come back to this
        //   expand bounding box()
        //   do some bounds checking?...   ???
        //   shrink bounding box()
        //   draw_hud_borders()
        // }
        // copy 0x253f/tmp to 0x2547/bbox
        // copy 0x2547/bbox/x0,y0 to 0x31ed/x0,y0
        // al <- 0x80 // top-left double border char
        // draw_modal_border() {
        //   print al
        //   al++
        //   for (i=0x31ed/x0; i<0x2547/x1; i++) print al
        //   al++
        //   print al
        // }
        // draw vertical edges
        // draw bottom border
        // shrink bbox
        // call fill_rectangle() and return
        return ip.incr(OPCODE + RECTANGLE);
    }
}
