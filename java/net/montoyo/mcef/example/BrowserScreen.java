package net.montoyo.mcef.example;

import org.lwjgl.opengl.GL11;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.montoyo.mcef.api.API;
import net.montoyo.mcef.api.IBrowser;
import net.montoyo.mcef.api.MCEFApi;

public class BrowserScreen extends GuiScreen {
	
	IBrowser browser = null;
	private GuiButton back = null;
	private GuiButton fwd = null;
	private GuiButton go = null;
	private GuiButton min = null;
	private GuiTextField url = null;
	
	@Override
	public void initGui() {
		if(browser == null) {
			//Grab the API and make sure it isn't null.
			API api = MCEFApi.getAPI();
			if(api == null)
				return;
			
			//Create a browser and resize it to fit the screen
			browser = api.createBrowser("https://www.google.com");
		}
		
		//Resize the browser if window size changed
		if(browser != null)
			browser.resize(mc.displayWidth, mc.displayHeight - scaleY(20));
		
		//Create GUI
		Keyboard.enableRepeatEvents(true);
		buttonList.clear();
		
		if(url == null) {
			buttonList.add(back = (new GuiButton(0, 0, 0, 20, 20, "<")));
			buttonList.add(fwd = (new GuiButton(1, 20, 0, 20, 20, ">")));
			buttonList.add(go = (new GuiButton(2, width - 40, 0, 20, 20, "Go")));
			buttonList.add(min = (new GuiButton(3, width - 20, 0, 20, 20, "_")));
			
			url = new GuiTextField(fontRendererObj, 40, 0, width - 80, 20);
			url.setMaxStringLength(65535);
			url.setText("http://www.google.com");
		} else {
			buttonList.add(back);
			buttonList.add(fwd);
			buttonList.add(go);
			buttonList.add(min);
			
			//Handle resizing
			go.xPosition = width - 40;
			min.xPosition = width - 20;
			
			String old = url.getText();
			url = new GuiTextField(fontRendererObj, 40, 0, width - 80, 20);
			url.setMaxStringLength(65535);
			url.setText(old);
		}
	}
	
	public int scaleY(int y) {
		double sy = ((double) y) / ((double) height) * ((double) mc.displayHeight);
		return (int) sy;
	}
	
	public void loadURL(String url) {
		browser.loadURL(url);
	}
	
	@Override
	public void drawScreen(int i1, int i2, float f) {
		//Render the URL box first because it overflows a bit
		url.drawTextBox();
		
		//Render buttons
		super.drawScreen(i1, i2, f);
		
		//Renders the browser if itsn't null
		if(browser != null) {
			GL11.glDisable(GL11.GL_DEPTH_TEST);
			browser.draw(.0d, height, width, 20.d); //Don't forget to flip Y axis.
			GL11.glEnable(GL11.GL_DEPTH_TEST);
		}
	}
	
	@Override
	public void onGuiClosed() {
		//Make sure to close the browser when you don't need it anymore.
		if(!ExampleMod.INSTANCE.hasBackup() && browser != null)
			browser.close();
		
		Keyboard.enableRepeatEvents(false);
	}
	
	@Override
	public void handleInput() {
		while(Keyboard.next()) {
			if(Keyboard.getEventKey() == Keyboard.KEY_ESCAPE) {
				mc.displayGuiScreen(null);
				return;
			}
			
			char key = Keyboard.getEventCharacter();
			int num = Keyboard.getEventKey();
			
			if(browser != null) { //Inject events into browser. TODO: Handle mods.
				boolean pressed = Keyboard.getEventKeyState();
				
				if(key != '.' && key != ';' && key != ',') { //Workaround
					if(pressed)
						browser.injectKeyPressed(key, 0);
					else
						browser.injectKeyReleased(key, 0);
				}
				
				if(key != Keyboard.CHAR_NONE)
					browser.injectKeyTyped(key, 0);
			}
			
			//Forward event to text box.
			url.textboxKeyTyped(key, num);
		}
		
		while(Mouse.next()) {
			int btn = Mouse.getEventButton();
			boolean pressed = Mouse.getEventButtonState();
			int sx = Mouse.getEventX();
			int sy = Mouse.getEventY();
			
			if(browser != null) { //Inject events into browser. TODO: Handle mods & leaving.
				int y = mc.displayHeight - sy - scaleY(20); //Don't forget to flip Y axis.
				
				if(btn == -1)
					browser.injectMouseMove(sx, y, 0, y < 0);
				else
					browser.injectMouseButton(sx, y, 0, btn + 1, pressed, 1);
			}
			
			if(pressed) { //Forward events to GUI.
				int x = sx * width / mc.displayWidth;
				int y = height - (sy * height / mc.displayHeight) - 1;
				
				mouseClicked(x, y, btn);
				url.mouseClicked(x, y, btn);
			}
		}
	}
	
	//Called by ExampleMod when the current browser's URL changes.
	public void onUrlChanged(IBrowser b, String nurl) {
		if(b == browser && url != null)
			url.setText(nurl);
	}
	
	//Handle button clicks
	@Override
	protected void actionPerformed(GuiButton src) {
		if(browser == null)
			return;
		
		if(src.id == 0)
			browser.goBack();
		else if(src.id == 1)
			browser.goForward();
		else if(src.id == 2)
			browser.loadURL(url.getText());
		else if(src.id == 3) {
			ExampleMod.INSTANCE.setBackup(this);
			mc.displayGuiScreen(null);
		}
	}

}
