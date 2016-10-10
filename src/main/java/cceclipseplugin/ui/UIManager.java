package cceclipseplugin.ui;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.part.ViewPart;

import cceclipseplugin.ui.dialogs.RegisterDialog;
import cceclipseplugin.ui.dialogs.WelcomeDialog;

public class UIManager {
	private ViewPart controlView;
	private Dialog welcomePrompt;
	private Dialog registerPrompt;
	
	public void setControlView(ViewPart view) {
		this.controlView = view;
	}
	
	public void popupWelcomePrompt() {
		welcomePrompt = new WelcomeDialog(new Shell());
		welcomePrompt.open();
	}
	
	public void popupRegisterPrompt() {
		registerPrompt = new RegisterDialog(new Shell());
		registerPrompt.open();
	}
}
