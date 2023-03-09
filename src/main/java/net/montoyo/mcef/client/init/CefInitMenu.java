package net.montoyo.mcef.client.init;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Matrix4f;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.montoyo.mcef.utilities.CefUtil;
import net.montoyo.mcef.utilities.IProgressListener;

import java.util.concurrent.atomic.AtomicInteger;

public class CefInitMenu extends Screen {
	// TODO:
	
	private static String[] text = new String[]{"", "", ""};
	
	private static double progress = 0;
	private static AtomicInteger isDone = new AtomicInteger(0);
	
	public static final IProgressListener listener = new IProgressListener() {
		@Override
		public void onProgressed(double d) {
			progress = d;
		}
		
		@Override
		public void onTaskChanged(String name) {
			int c = name.charAt(0) - '1';
			name = name.substring(2);
			text[c] = name;
		}
		
		@Override
		public void onProgressEnd() {
			isDone.addAndGet(1);
		}
	};
	
	TitleScreen menu;
	
	public CefInitMenu(TitleScreen menu) {
//		super(Component.translatable("mcef.menu.init"));
		super(Component.literal("MCEF hasn't finished downloading CEF yet"));
		this.menu = menu;
	}
	
	private static void blit(Matrix4f matrix, int x0, int x1, int y0, int y1, int z, float r, float g, float b, float a) {
		RenderSystem.setShader(GameRenderer::getPositionColorShader);
		BufferBuilder bufferbuilder = Tesselator.getInstance().getBuilder();
		bufferbuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
		bufferbuilder.vertex(matrix, (float) x0, (float) y1, (float) z).color(r, g, b, a).endVertex();
		bufferbuilder.vertex(matrix, (float) x1, (float) y1, (float) z).color(r, g, b, a).endVertex();
		bufferbuilder.vertex(matrix, (float) x1, (float) y0, (float) z).color(r, g, b, a).endVertex();
		bufferbuilder.vertex(matrix, (float) x0, (float) y0, (float) z).color(r, g, b, a).endVertex();
		BufferUploader.drawWithShader(bufferbuilder.end());
	}
	
	@Override
	public void render(PoseStack p_96562_, int p_96563_, int p_96564_, float p_96565_) {
		renderBackground(p_96562_);
		double cx = width / 2d;
		double cy = height / 2d;
		
		double progressBarHeight = 14;
		double progressBarWidth = width / 3d; // TODO: base off screen with (1/3 of screen)
		
		RenderSystem.disableTexture();
		p_96562_.pushPose();
		p_96562_.translate(cx, cy, 0);
		p_96562_.translate(-progressBarWidth / 2d, -progressBarHeight / 2d, 0);
		Matrix4f matrix = p_96562_.last().pose();
		blit(
				matrix,
				0, (int) progressBarWidth,
				0, (int) progressBarHeight,
				1,
				1, 1, 1, 1
		);
		blit(
				matrix,
				2, (int) progressBarWidth - 2,
				2, (int) progressBarHeight - 2,
				1,
				0, 0, 0, 1
		);
		blit(
				matrix,
				4, (int) ((progressBarWidth - 4) * progress),
				4, (int) progressBarHeight - 4,
				1,
				1, 1, 1, 1
		);
		p_96562_.popPose();
		RenderSystem.enableTexture();
		
		int oSet = (int) ((font.lineHeight / 2) + ((font.lineHeight + 2) * 4)) + 4;
		p_96562_.pushPose();
		p_96562_.translate(
				(int) (cx),
				(int) (cy - oSet),
				0
		);
		drawString(
				p_96562_, font,
				title,
				(int) -(font.width(title) / 2d), 0,
				0xFFFFFF
		);
		for (String s : text) {
			p_96562_.translate(0, font.lineHeight + 2, 0);
			drawString(
					p_96562_, font,
					s,
					(int) -(font.width(s) / 2d), 0,
					0xFFFFFF
			);
		}
		p_96562_.popPose();
	}
	
	@Override
	public void tick() {
		if (isDone.get() == 1) {
			Minecraft.getInstance().setScreen(menu);
			CefUtil.runInit();
		}
	}
}
