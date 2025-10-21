package fpt.edu.vn.smartpantrychef;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;

import com.facebook.shimmer.ShimmerFrameLayout;
import com.google.android.material.chip.Chip;
import com.google.android.material.snackbar.Snackbar;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.label.ImageLabel;
import com.google.mlkit.vision.label.ImageLabeler;
import com.google.mlkit.vision.label.ImageLabeling;
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;

import fpt.edu.vn.smartpantrychef.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    private ActivityMainBinding binding;
    private Bitmap capturedBitmap;

    // Trình khởi chạy yêu cầu quyền
    private ActivityResultLauncher<String> requestCameraPermissionLauncher;
    private ActivityResultLauncher<String> requestGalleryPermissionLauncher;

    // Trình khởi chạy camera và thư viện
    private ActivityResultLauncher<Void> cameraLauncher;
    private ActivityResultLauncher<Intent> galleryLauncher;

    // ML Kit Image Labeler
    private ImageLabeler labeler;

    private MainViewModel viewModel;
    private List<String> detectedIngredients = new ArrayList<>();
    private ShimmerFrameLayout shimmerLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Khởi tạo ML Kit labeler
        labeler = ImageLabeling.getClient(ImageLabelerOptions.DEFAULT_OPTIONS);

        // Khởi tạo trình khởi chạy yêu cầu quyền camera
        requestCameraPermissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
            if (isGranted) {
                Toast.makeText(this, "Quyền camera đã được cấp", Toast.LENGTH_SHORT).show();
                openCamera();
            } else {
                showPermissionDeniedDialog("Cần quyền camera để chụp ảnh nguyên liệu.");
            }
        });

        // Khởi tạo trình khởi chạy yêu cầu quyền truy cập thư viện
        requestGalleryPermissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
            if (isGranted) {
                Toast.makeText(this, "Quyền truy cập thư viện đã được cấp", Toast.LENGTH_SHORT).show();
                openGallery();
            } else {
                showPermissionDeniedDialog("Cần quyền truy cập thư viện để chọn ảnh.");
            }
        });

        // Khởi tạo trình khởi chạy camera
        cameraLauncher = registerForActivityResult(new ActivityResultContracts.TakePicturePreview(), this::handleImageResult);

        // Khởi tạo trình khởi chạy thư viện
        galleryLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                Uri imageUri = result.getData().getData();
                try {
                    Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), imageUri);
                    handleImageResult(bitmap);
                } catch (IOException e) {
                    Log.e(TAG, "Error converting URI to Bitmap", e);
                    Toast.makeText(this, "Không thể tải ảnh từ thư viện", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this, "Đã hủy chọn ảnh", Toast.LENGTH_SHORT).show();
            }
        });

        // Initialize ViewModel
        viewModel = new ViewModelProvider(this).get(MainViewModel.class);

        // Observe isLoading LiveData
        viewModel.getIsLoading().observe(this, isLoading -> {
            if (isLoading != null && isLoading) {
                binding.progressBar.setVisibility(View.VISIBLE);
                binding.btnFindRecipe.setEnabled(false);
            } else {
                binding.progressBar.setVisibility(View.GONE);
                binding.btnFindRecipe.setEnabled(true);
            }
        });

        // Observe recipeResponse LiveData
        viewModel.getRecipeResponse().observe(this, recipe -> {
            if (recipe != null && !recipe.isEmpty()) {
                Intent intent = new Intent(MainActivity.this, RecipeActivity.class);
                intent.putExtra("RECIPE_TEXT", recipe);
                startActivity(intent);
            }
        });

        // Observe errorMessage LiveData
        viewModel.getErrorMessage().observe(this, error -> {
            if (error != null) {
                Toast.makeText(this, "Lỗi: " + error, Toast.LENGTH_LONG).show();
            }
        });

        // Thiết lập sự kiện click
        binding.tvEmptyState.setOnClickListener(v -> openCamera());
        binding.btnChooseFromGallery.setOnClickListener(v -> openGallery());
        binding.btnFindRecipe.setOnClickListener(v -> {
            try {
                detectedIngredients.clear();
                for (int i = 0; i < binding.chipGroupIngredients.getChildCount(); i++) {
                    View chipView = binding.chipGroupIngredients.getChildAt(i);
                    if (chipView instanceof Chip) {
                        CharSequence text = ((Chip) chipView).getText();
                        if (text != null && !text.toString().trim().isEmpty()) {
                            detectedIngredients.add(text.toString().trim());
                        }
                    }
                }
                if (!detectedIngredients.isEmpty()) {
                    if (!isNetworkAvailable()) {
                        showError("Không có kết nối mạng. Vui lòng kiểm tra lại.");
                        return;
                    }
                    viewModel.getRecipes(detectedIngredients);
                } else {
                    showError("Chưa có nguyên liệu nào");
                }
            } catch (Exception e) {
                Log.e(TAG, "Error collecting ingredients", e);
                showError("Lỗi khi lấy nguyên liệu: " + e.getMessage());
            }
        });

        shimmerLayout = findViewById(R.id.shimmerLayout);
    }

    // Xử lý kết quả ảnh (từ camera hoặc thư viện)
    private void handleImageResult(Bitmap bitmap) {
        if (bitmap != null) {
            try {
                capturedBitmap = bitmap;
                binding.tvEmptyState.setVisibility(View.GONE);
                binding.cardPreview.setVisibility(View.VISIBLE);
                binding.buttonsLayout.setVisibility(View.VISIBLE);
                binding.imageViewPreview.setImageBitmap(capturedBitmap);
                binding.imageViewPreview.startAnimation(AnimationUtils.loadAnimation(this, R.anim.fade_in));
                startShimmer();
                showError("Đang phân tích ảnh...");
                analyzeImage(capturedBitmap);
            } catch (Exception e) {
                Log.e(TAG, "Error processing image result", e);
                showError("Có lỗi xảy ra khi xử lý ảnh: " + e.getMessage());
                stopShimmer();
            }
        } else {
            showError("Không nhận được ảnh");
            stopShimmer();
        }
    }

    // Kiểm tra và yêu cầu quyền truy cập camera
    private void openCamera() {
        if (checkCameraPermission()) {
            try {
                cameraLauncher.launch(null);
            } catch (Exception e) {
                Log.e(TAG, "Cannot open camera", e);
                Toast.makeText(this, "Không thể mở camera", Toast.LENGTH_SHORT).show();
            }
        }
    }

    // Kiểm tra và yêu cầu quyền truy cập thư viện
    private void openGallery() {
        if (checkGalleryPermission()) {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            galleryLauncher.launch(intent);
        }
    }

    // Kiểm tra quyền truy cập camera
    private boolean checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            return true;
        } else {
            requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA);
            return false;
        }
    }

    // Kiểm tra quyền truy cập thư viện
    private boolean checkGalleryPermission() {
        String permission = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU ?
                Manifest.permission.READ_MEDIA_IMAGES : Manifest.permission.READ_EXTERNAL_STORAGE;

        if (ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED) {
            return true;
        } else {
            requestGalleryPermissionLauncher.launch(permission);
            return false;
        }
    }

    // Hiển thị hộp thoại khi quyền bị từ chối
    private void showPermissionDeniedDialog(String message) {
        new AlertDialog.Builder(this)
                .setTitle("Yêu cầu quyền")
                .setMessage(message + " Vui lòng cấp quyền trong cài đặt ứng dụng.")
                .setPositiveButton("OK", (dialog, which) -> dialog.dismiss())
                .create()
                .show();
    }

    private void showError(String message) {
        Snackbar snackbar = Snackbar.make(binding.getRoot(), message, Snackbar.LENGTH_LONG);
        View sbView = snackbar.getView();
        sbView.setBackgroundColor(0xFFD32F2F); // Red background
        snackbar.setAction("Thử lại", v -> {
            // Retry logic can be implemented here if needed
        });
        snackbar.show();
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        if (cm != null) {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                NetworkCapabilities nc = cm.getNetworkCapabilities(cm.getActiveNetwork());
                return nc != null && (nc.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) || nc.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR));
            } else {
                android.net.NetworkInfo ni = cm.getActiveNetworkInfo();
                return ni != null && ni.isConnectedOrConnecting(); // Sử dụng isConnectedOrConnecting thay cho isConnected
            }
        }
        return false;
    }

    // Phân tích ảnh bằng ML Kit
    private void analyzeImage(Bitmap bitmap) {
        if (bitmap == null) {
            showError("Ảnh không hợp lệ");
            stopShimmer();
            return;
        }
        binding.progressBar.setVisibility(View.GONE);
        binding.chipGroupIngredients.removeAllViews();
        try {
            InputImage image = InputImage.fromBitmap(bitmap, 0);
            labeler.process(image)
                    .addOnSuccessListener(labels -> {
                        ArrayList<String> detectedIngredients = new ArrayList<>();
                        for (ImageLabel label : labels) {
                            if (label.getConfidence() > 0.6f) {
                                String ingredient = label.getText();
                                detectedIngredients.add(ingredient);
                                addIngredientChip(ingredient);
                            }
                        }
                        stopShimmer();
                        if (!detectedIngredients.isEmpty()) {
                            binding.layoutIngredientsSection.setVisibility(View.VISIBLE);
                            binding.layoutIngredientsSection.startAnimation(AnimationUtils.loadAnimation(this, R.anim.slide_up));
                            binding.btnFindRecipe.setVisibility(View.VISIBLE);
                        } else {
                            showError("Không phát hiện nguyên liệu nào");
                        }
                    })
                    .addOnFailureListener(e -> {
                        stopShimmer();
                        Log.e(TAG, "Image labeling failed", e);
                        showError("Lỗi phân tích ảnh: " + e.getMessage());
                    });
        } catch (IllegalArgumentException e) {
            stopShimmer();
            Log.e(TAG, "Invalid image for ML Kit", e);
            showError("Ảnh không hợp lệ cho phân tích: " + e.getMessage());
        } catch (Exception e) {
            stopShimmer();
            Log.e(TAG, "Unexpected error in analyzeImage", e);
            showError("Lỗi không xác định khi phân tích ảnh: " + e.getMessage());
        }
    }

    // Thêm chip nguyên liệu vào ChipGroup
    private void addIngredientChip(String ingredient) {
        Chip chip = new Chip(this);
        chip.setText(ingredient);
        chip.setCloseIconVisible(true);
        chip.setOnCloseIconClickListener(v -> {
            chip.startAnimation(AnimationUtils.loadAnimation(this, R.anim.chip_scale_in));
            binding.chipGroupIngredients.removeView(chip);
        });
        chip.setBackgroundResource(R.drawable.chip_ripple);
        chip.startAnimation(AnimationUtils.loadAnimation(this, R.anim.chip_scale_in));
        binding.chipGroupIngredients.addView(chip);
    }

    private void startShimmer() {
        if (shimmerLayout != null) {
            try {
                shimmerLayout.setVisibility(View.VISIBLE);
                shimmerLayout.startShimmer();
            } catch (Exception e) {
                Log.e(TAG, "Shimmer error", e);
            }
        }
        binding.progressBar.setVisibility(View.GONE);
    }

    private void stopShimmer() {
        if (shimmerLayout != null) {
            try {
                shimmerLayout.stopShimmer();
                shimmerLayout.setVisibility(View.GONE);
            } catch (Exception e) {
                Log.e(TAG, "Shimmer error", e);
            }
        }
        binding.progressBar.setVisibility(View.GONE);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Clean up resources if needed
        // Example: labeler.close(); if labeler implements Closeable
    }
}

// HƯỚNG DẪN: Thêm vào build.gradle (Module: app):
// implementation 'com.facebook.shimmer:shimmer:0.5.0'
// Sau đó Sync lại Gradle để sử dụng ShimmerFrameLayout
