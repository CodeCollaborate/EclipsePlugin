package cceclipseplugin.ui;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.part.ViewPart;

public class UIManager {
	private Shell uiShell = new Shell();
	
	private ViewPart controlView;
	private Dialog welcomePrompt;
	private Dialog registerPrompt;
	
	public void setControlView(ViewPart view) {
		this.controlView = view;
	}
	
	public void popupWelcomePrompt() {
		welcomePrompt = new WelcomeDialog(uiShell);
		welcomePrompt.open();
	}
	
	public void popupRegisterPrompt() {
		registerPrompt = new RegisterDialog(uiShell);
		registerPrompt.open();
	}
}
