package au.org.intersect.faims.android.ui.drawer;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Stack;

import android.support.v4.widget.DrawerLayout;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import au.org.intersect.faims.android.R;
import au.org.intersect.faims.android.app.FAIMSApplication;
import au.org.intersect.faims.android.beanshell.BeanShellLinker;
import au.org.intersect.faims.android.beanshell.callbacks.ActionButtonCallback;
import au.org.intersect.faims.android.data.Module;
import au.org.intersect.faims.android.log.FLog;
import au.org.intersect.faims.android.ui.activity.ShowModuleActivity;
import au.org.intersect.faims.android.ui.view.TabGroup;
import au.org.intersect.faims.android.ui.view.UIRenderer;
import au.org.intersect.faims.android.util.Arch16n;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class NavigationDrawer {
	
	private static final String DANGER = "danger";
	private static final String SUCCESS = "success";
	private static final String PRIMARY = "primary";

	@Inject
	UIRenderer uiRenderer;
	
	@Inject
	BeanShellLinker beanShellLinker;
	
	@Inject
	Arch16n arch16n;

	private WeakReference<ShowModuleActivity> activityRef;
	private DrawerLayout naviationDrawerLayout;
	private TextView moduleNameText;
	private TextView moduleDescriptionText;
	private LinearLayout navigationStackLayout;
	private LinearLayout navigationButtonsLayout;
	
	private HashMap<String, Button> actionButtons;

	private Stack<TabGroup> tabGroupStack;

	public void init(ShowModuleActivity activity) {
		FAIMSApplication.getInstance().injectMembers(this);
		
		activityRef = new WeakReference<ShowModuleActivity>(activity);
		
		naviationDrawerLayout = (DrawerLayout) activity.findViewById(R.id.navigation_drawer_layout);
		
		moduleNameText = (TextView) activity.findViewById(R.id.module_name);
		moduleDescriptionText = (TextView) activity.findViewById(R.id.module_description);
		navigationStackLayout = (LinearLayout) activity.findViewById(R.id.navigation_stack);
		navigationButtonsLayout = (LinearLayout) activity.findViewById(R.id.navigation_buttons);
		
		Module module = activityRef.get().getModule();
		moduleNameText.setText(module.getName());
		moduleDescriptionText.setText(module.getDescription());
		
		tabGroupStack = new Stack<TabGroup>();
		actionButtons = new HashMap<String, Button>();
		
		setupExitAction();
		setupHomeAction();
	}
	
	public void pushTabGroup(TabGroup tabGroup) {
		tabGroupStack.push(tabGroup);
		update();
	}
	
	public void popTabGroup() {
		tabGroupStack.pop();
		update();
	}
	
	public void popTabGroupNoUpdate() {
		tabGroupStack.pop();
	}
	
	public void update() {
		navigationStackLayout.removeAllViews();
		Button button = (Button) LayoutInflater.from(navigationStackLayout.getContext()).inflate(R.layout.tab_group_header_button, null);
		button.setText("Tab Groups");
		navigationStackLayout.addView(button);
		
		for (int i = 0; i < tabGroupStack.size(); i++) {
			TabGroup tabGroup = (TabGroup) tabGroupStack.get(i);
			final int tabGroupIndex = i;
			Button groupButton = (Button) LayoutInflater.from(navigationStackLayout.getContext()).inflate(R.layout.tab_group_button, null);
			groupButton.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View v) {
					uiRenderer.navigateToTabGroup(tabGroupIndex);
					naviationDrawerLayout.closeDrawer(Gravity.START);
				}
				
			});
			groupButton.setText(arch16n.substituteValue(tabGroup.getLabel()));
			if (i == tabGroupStack.size() - 1) {
				groupButton.setEnabled(false);
			}
			navigationStackLayout.addView(groupButton);
		}
		activityRef.get().updateActionBarTitle();
	}
	
	private void setupExitAction() {
		Button button = (Button) activityRef.get().findViewById(R.id.exit_button);
		button.setText("Exit Module");
		button.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				activityRef.get().closeActivity();
			}
		});
	}
	
	private void setupHomeAction() {
		Button button = (Button) activityRef.get().findViewById(R.id.home_button);
		button.setText("Module Home");
		button.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				uiRenderer.navigateToTabGroup(0);
				naviationDrawerLayout.closeDrawer(Gravity.START);
			}
		});
	}
	
	public void addNavigationAction(final String name, final ActionButtonCallback callback, String type) {
		Button button = (Button) LayoutInflater.from(navigationButtonsLayout.getContext()).inflate(getButtonLayout(type), null);
		button.setText(arch16n.substituteValue(callback.actionOnLabel()));
		button.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				try {
					callback.actionOn();
				} catch (Exception e) {
					showActionError(callback, e);
				}
			}
		});
		navigationButtonsLayout.addView(button);
		
		actionButtons.put(name, button);
	}
	
	private int getButtonLayout(String type) {
		if (PRIMARY.equals(type)) {
			return R.layout.button_primary;
		} else if (SUCCESS.equals(type)) {
			return R.layout.button_success;
		} else if (DANGER.equals(type)) {
			return R.layout.button_danger;
		} else {
			return R.layout.button_default;
		}		
	}

	public void removeNavigationAction(String name) {
		Button button = actionButtons.remove(name);
		if (button != null) {
			navigationButtonsLayout.removeView(button);
		}
	}
	
	private void showActionError(ActionButtonCallback callback, Exception e) {
		try {
			FLog.e("error trying to call navigation action actionon callback", e);
			callback.onError(e.getMessage());
		} catch (Exception ce) {
			FLog.e("error trying to call navigation action onerror callback", ce);
			beanShellLinker.reportError("error trying to call navigation action onerror callback", ce);
		}
	}

	public TabGroup peekTabGroup() {
		if (tabGroupStack.size() > 0) {
			return tabGroupStack.peek();
		}
		return null;
	}

	public int getTabGroupCount() {
		return tabGroupStack.size();
	}
	
	public Stack<TabGroup> getTabGroupStack() {
		return tabGroupStack;
	}
	
}
