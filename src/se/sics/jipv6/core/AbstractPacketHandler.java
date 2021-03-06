/**
 * Copyright (c) 2009, Swedish Institute of Computer Science.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 * 3. Neither the name of the Institute nor the names of its contributors
 *    may be used to endorse or promote products derived from this software
 *    without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE INSTITUTE AND CONTRIBUTORS ``AS IS'' AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED.  IN NO EVENT SHALL THE INSTITUTE OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS
 * OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
 * OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 *
 * This file is part of jipv6.
 *
 * $Id: $
 *
 * -----------------------------------------------------------------
 *
 *
 * Author  : Joakim Eriksson
 * Created :  mar 2009
 * Updated : $Date:$
 *           $Revision:$
 */

package se.sics.jipv6.core;

import java.io.PrintStream;
import java.util.Vector;

public abstract class AbstractPacketHandler implements PacketHandler {

    boolean debug = false;

    Vector<PacketHandlerDispatch> upperLayers = new Vector<PacketHandlerDispatch>();
    protected PacketHandler lowerLayer;

    public PacketHandler getLowerLayerHandler() {
        return lowerLayer;
    }

    public void addUpperLayerHandler(int protoID, PacketHandler handler) {
        PacketHandlerDispatch layer = new PacketHandlerDispatch();
        layer.dispatch = protoID;
        layer.packetHandler = handler;
        upperLayers.addElement(layer);
    }

    public void setLowerLayerHandler(PacketHandler handler) {
        lowerLayer = handler;
    }

    /* incoming packets ... */
    protected void dispatch(int dispatch, MacPacket packet) {
        if (debug) {
            printPacket(System.out, packet);
        }
        if (dispatch != -1) {
            for (int i = 0; i < upperLayers.size(); i++) {
                if (upperLayers.elementAt(i).dispatch == dispatch) {
                    upperLayers.elementAt(i).packetHandler.packetReceived(packet);
                    return;
                }
            }
            System.out.println("**** no dispatch handler for " + dispatch + " found...");
        } else if (upperLayers.size() > 0){
            upperLayers.elementAt(0).packetHandler.packetReceived(packet);
        }
    }

    public abstract void packetReceived(MacPacket container);

    public abstract void sendPacket(MacPacket packet);

    public void printPacket(PrintStream out, MacPacket packet) {
    }

    public static class PacketHandlerDispatch {
        int dispatch;
        PacketHandler packetHandler;
    }
}
