package il.ronmad.speedruntimer;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;
import android.util.Log;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;

@RunWith(AndroidJUnit4.class)
public class InstrumentedInstalledAppsTest {

    private Context appContext;

    @Before
    public void setUp() {
        appContext = InstrumentationRegistry.getTargetContext();
    }

    @Test
    public void testInstalledGames() {
        final PackageManager pm = appContext.getPackageManager();
        List<ApplicationInfo> packages = pm.getInstalledApplications(PackageManager.GET_META_DATA);
        for (ApplicationInfo packageInfo : packages) {
            if (packageInfo.category != ApplicationInfo.CATEGORY_GAME) continue;
            if (packageInfo.packageName.startsWith("com.google")) continue;
            Log.d("testInstalledGames", "Name: " + pm.getApplicationLabel(packageInfo));
            Log.d("testInstalledGames", "Installed package: " + packageInfo.packageName);
            Log.d("testInstalledGames", "Launch Activity: " + pm.getLaunchIntentForPackage(packageInfo.packageName));
        }
    }
}
