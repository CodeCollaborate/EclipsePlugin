package cceclipseplugin.ui;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

public class ListView extends Composite {
	
	private Composite header;
	private HListWithVButtons listWithButtons;

	public ListView(Composite parent, int style, String header) {
		super(parent, style);
		this.initialize(header);
	}

	private void initialize(String header) {
		GridLayout layout = new GridLayout(1, false);
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		layout.horizontalSpacing = 0;
		layout.verticalSpacing = 0;
		this.setLayout(layout);
		this.header = this.createHeader(header);
		this.listWithButtons = this.createListWithButtons();
	}
	
	private Composite createHeader(String title) {
		// we are wrapping the header's label in a composite because
		// the SWT border for labels looks stupid (imagine a pressed button)
		Composite header = new Composite(this, SWT.BORDER);
		GridData data = new GridData();
		data.grabExcessHorizontalSpace = true;
		data.horizontalAlignment = GridData.FILL;
		header.setLayoutData(data);
		FillLayout layout = new FillLayout();
		header.setLayout(layout);
		Label headerLabel = new Label(header, SWT.NONE);
		headerLabel.setText(title);
		return header;
	}
	
	private HListWithVButtons createListWithButtons() {
		HListWithVButtons buttonList = new HListWithVButtons(this, SWT.NONE);
		GridData data = new GridData();
		data.grabExcessHorizontalSpace = true;
		data.horizontalAlignment = GridData.FILL;
		data.grabExcessVerticalSpace = true;
		data.verticalAlignment = GridData.FILL;
		buttonList.setLayoutData(data);
		return buttonList;
	}

	protected Composite getHeader() {
		return header;
	}

	protected HListWithVButtons getListWithButtons() {
		return listWithButtons;
	}
}