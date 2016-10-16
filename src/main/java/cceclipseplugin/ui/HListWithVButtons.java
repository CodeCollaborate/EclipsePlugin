package cceclipseplugin.ui;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.List;

public class HListWithVButtons extends Composite {
	
	private List list;
	private VerticalButtonBar buttonBar;
	
	public HListWithVButtons(Composite parent, int style) {
		super(parent, style);
		GridLayout gridLayout = new GridLayout();
		gridLayout.numColumns = 2;
		gridLayout.horizontalSpacing = 0;
		gridLayout.marginWidth = 0;
		gridLayout.marginHeight = 0;
		this.setLayout(gridLayout);
		this.list = this.createList();
		this.buttonBar = this.createButtonBar();
	}
	
	private List createList() {
		List listy = new List(this, SWT.BORDER);
		GridData data = new GridData();
		data.grabExcessHorizontalSpace = true;
		data.horizontalAlignment = GridData.FILL;
		data.grabExcessVerticalSpace = true;
		data.verticalAlignment = GridData.FILL;
		listy.setLayoutData(data);
		return listy;
	}
	
	private VerticalButtonBar createButtonBar() {
		VerticalButtonBar buttonBar = new VerticalButtonBar(this, SWT.BORDER);
		GridData data = new GridData();
		data.verticalAlignment = GridData.VERTICAL_ALIGN_CENTER;
		buttonBar.setLayoutData(data);
		return buttonBar;
	}
	
	List getList() {
		return list;
	}
	
	VerticalButtonBar getButtonBar() {
		return buttonBar;
	}
	
	@Override
	public void setEnabled(boolean b) {
		if (b) {
			if (list.getItemCount() <= 0)
				buttonBar.getMinusButton().setEnabled(!b);
			else
				buttonBar.getMinusButton().setEnabled(b);
			buttonBar.getPlusButton().setEnabled(b);;
		} else {
			buttonBar.getPlusButton().setEnabled(b);
			buttonBar.getMinusButton().setEnabled(b);
		}
		super.setEnabled(b);	
	}
}
