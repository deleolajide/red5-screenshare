package org.redfire.screen;

import java.awt.image.*;


public interface ScreenCodec {

	public byte[]  encode(BufferedImage bufferedimage) throws Exception;


}