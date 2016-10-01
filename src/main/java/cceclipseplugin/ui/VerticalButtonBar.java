package cceclipseplugin.ui;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;

public class VerticalButtonBar extends Composite {
	
	private Button plusButton;
	private Button minusButton;
	
	public VerticalButtonBar(Composite parent, int style) {
		super(parent, style);
		this.initialize();
	}
	
	private void initialize() {
		GridLayout gridLayout = new GridLayout(1, true);
		gridLayout.verticalSpacing = 2;
		gridLayout.marginHeight = 2;
		gridLayout.marginWidth = 2;
		this.setLayout(gridLayout);
		
		plusButton = new Button(this, SWT.NONE);
		GridData data = new GridData(SWT.CENTER, SWT.CENTER, false, false, 1, 1);
		data.heightHint = 25;
		data.widthHint = 25;
		plusButton.setLayoutData(data);
		plusButton.setText("+");
		
		minusButton = new Button(this, SWT.NONE);
		GridData data2 = new GridData(SWT.CENTER, SWT.CENTER, false, false, 1, 1);
		data2.heightHint = 25;
		data2.widthHint = 25;
		minusButton.setLayoutData(data2);
		minusButton.setText("-");
	}
	
	public Button getPlusButton() {
		return plusButton;
	}
	
	public Button getMinusButton() {
		return minusButton;
	}
}