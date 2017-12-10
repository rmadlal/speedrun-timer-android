package il.ronmad.speedruntimer;

import android.content.Context;
import android.graphics.Rect;
import android.os.AsyncTask;
import android.support.v7.widget.AppCompatAutoCompleteTextView;
import android.util.AttributeSet;
import android.widget.ArrayAdapter;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.InputStreamReader;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

public class CategoryAutoCompleteView extends AppCompatAutoCompleteTextView {

    static Map<String, String[]> categoryCache = new HashMap<>();

    public CategoryAutoCompleteView(Context context) {
        super(context);
    }

    public CategoryAutoCompleteView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public CategoryAutoCompleteView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    void setCategories(String gameName) {
        if (categoryCache.containsKey(gameName)) {
            setAdapter(new ArrayAdapter<>(getContext(),
                    R.layout.autocomplete_dropdown_item, categoryCache.get(gameName)));
        } else {
            new FetchSrcTask(this).execute(gameName);
        }
    }

    @Override
    protected void onFocusChanged(boolean focused, int direction, Rect previouslyFocusedRect) {
        super.onFocusChanged(focused, direction, previouslyFocusedRect);

        if (getWindowVisibility() != VISIBLE) {
            return;
        }
        if (focused) {
            if (getError() == null) {
                showDropDown();
            }
        } else {
            dismissDropDown();
        }
    }

    @Override
    public boolean enoughToFilter() {
        return true;
    }

    static class FetchSrcTask extends AsyncTask<String, Void, String[]> {

        private static String SRC_API = "https://www.speedrun.com/api/v1";

        private WeakReference<CategoryAutoCompleteView> instance;
        private String gameName;

        FetchSrcTask(CategoryAutoCompleteView view) {
            this.instance = new WeakReference<>(view);
        }

        @Override
        protected String[] doInBackground(String... gameNames) {
            gameName = gameNames[0];
            String[] categories;
            try {
                String gameNameUrl = URLEncoder.encode(gameName, "UTF-8");
                JsonObject json = getJson(new URL(SRC_API + "/games?name=" + gameNameUrl));
                JsonObject gameData = json.get("data").getAsJsonArray().get(0).getAsJsonObject();

                String srcName = gameData.get("names").getAsJsonObject().get("international").getAsString();
                if (!srcName.toLowerCase().equals(gameName.toLowerCase())) {
                    return new String[]{"Any%", "100%", "Low%"};
                }
                JsonArray linksArray = gameData.get("links").getAsJsonArray();
                String categoriesUrl = "";
                for (JsonElement linkElement : linksArray) {
                    JsonObject linkObject = linkElement.getAsJsonObject();
                    if (linkObject.get("rel").getAsString().equals("categories")) {
                        categoriesUrl = linkObject.get("uri").getAsString();
                        break;
                    }
                }
                json = getJson(new URL(categoriesUrl));
                JsonArray categoriesData = json.get("data").getAsJsonArray();
                categories = new String[categoriesData.size()];
                for (int i = 0; i < categories.length; i++) {
                    JsonObject categoryObject = categoriesData.get(i).getAsJsonObject();
                    categories[i] = categoryObject.get("name").getAsString();
                }
            } catch (Exception e) {
                return new String[]{"Any%", "100%", "Low%"};
            }
            return categories;
        }

        @Override
        protected void onPostExecute(String[] results) {
            CategoryAutoCompleteView view = instance.get();
            CategoryAutoCompleteView.categoryCache.put(gameName, results);
            view.setAdapter(new ArrayAdapter<>(view.getContext(),
                    R.layout.autocomplete_dropdown_item, results));
            if (view.isShown()) {
                view.showDropDown();
            }
        }

        private JsonObject getJson(URL url) throws Exception {
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            InputStreamReader reader = new InputStreamReader(conn.getInputStream());
            return new JsonParser().parse(reader).getAsJsonObject();
        }
    }
}
