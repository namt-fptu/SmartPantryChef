package fpt.edu.vn.smartpantrychef;

import android.app.Application;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.google.ai.client.generativeai.GenerativeModel;
import com.google.ai.client.generativeai.java.GenerativeModelFutures;
import com.google.ai.client.generativeai.type.Content;
import com.google.ai.client.generativeai.type.GenerateContentResponse;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class MainViewModel extends AndroidViewModel {

    private static final String TAG = "MainViewModel";
    private static final String API_KEY = "AIzaSyAAb-1TeJpIvdmILCaqu3zWas5IkG8Sh_Q";

    private final MutableLiveData<String> recipeResponse = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>();
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();

    private final Executor executor;
    private final GenerativeModelFutures model;

    public MainViewModel(@NonNull Application application) {
        super(application);
        executor = Executors.newSingleThreadExecutor();

        // ✅ Khởi tạo model Gemini SDK 0.9.0
        GenerativeModel gm = new GenerativeModel("gemini-1.5-flash", API_KEY);
        model = GenerativeModelFutures.from(gm);
    }

    public LiveData<String> getRecipeResponse() {
        return recipeResponse;
    }

    public LiveData<Boolean> getIsLoading() {
        return isLoading;
    }

    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }

    // 🚀 Hàm gọi API (SDK 0.9.0)
    public void getRecipes(List<String> ingredients) {
        isLoading.postValue(true);

        String prompt = "Bạn là đầu bếp chuyên nghiệp cho sinh viên. Hãy đề xuất 2 công thức nấu ăn:\n" +
                "- Đơn giản, dễ làm dưới 20 phút\n" +
                "- Chi phí thấp\n" +
                "- Dùng nguyên liệu: " + ingredients.toString() + "\n\n" +
                "Format:\nMón 1:\nNguyên liệu:\n- ...\nCách làm:\n1. ...";

        Content content = new Content.Builder()
                .addText(prompt)
                .build();

        // ✅ Sử dụng ListenableFuture (API mới)
        ListenableFuture<GenerateContentResponse> response = model.generateContent(content);

        Futures.addCallback(response, new FutureCallback<GenerateContentResponse>() {
            @Override
            public void onSuccess(GenerateContentResponse result) {
                try {
                    String text = result.getText();

                    if (text != null && !text.trim().isEmpty()) {
                        recipeResponse.postValue(text.trim());
                    } else {
                        errorMessage.postValue("⚠️ Không nhận được nội dung hợp lệ từ Gemini.");
                    }
                } catch (Exception e) {
                    Log.e(TAG, "❌ Lỗi xử lý phản hồi Gemini", e);
                    errorMessage.postValue("Lỗi xử lý phản hồi: " + e.getMessage());
                } finally {
                    isLoading.postValue(false);
                }
            }

            @Override
            public void onFailure(@NonNull Throwable t) {
                Log.e(TAG, "❌ Lỗi gọi Gemini API", t);
                errorMessage.postValue("Lỗi API: " + t.getMessage());
                isLoading.postValue(false);
            }
        }, executor);
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        if (executor instanceof java.util.concurrent.ExecutorService) {
            ((java.util.concurrent.ExecutorService) executor).shutdown();
        }
    }
}