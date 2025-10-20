package fpt.edu.vn.smartpantrychef;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;

import fpt.edu.vn.smartpantrychef.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity {

    // Khai báo hằng số cho mã yêu cầu quyền camera
    private static final int CAMERA_PERMISSION_CODE = 100;

    // ViewBinding để truy cập các view trong layout
    private ActivityMainBinding binding;

    // Bitmap để lưu ảnh đã chụp
    private Bitmap capturedBitmap;

    // Trình khởi chạy yêu cầu quyền
    private ActivityResultLauncher<String> requestPermissionLauncher;

    // Trình khởi chạy camera
    private ActivityResultLauncher<Void> cameraLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Khởi tạo ViewBinding
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Khởi tạo trình khởi chạy yêu cầu quyền và xử lý kết quả
        requestPermissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
            if (isGranted) {
                // Quyền đã được cấp, thông báo cho người dùng và mở camera
                Toast.makeText(this, "Quyền camera đã được cấp", Toast.LENGTH_SHORT).show();
                openCamera();
            } else {
                // Quyền bị từ chối, hiển thị hộp thoại giải thích
                new AlertDialog.Builder(this)
                        .setTitle("Yêu cầu quyền")
                        .setMessage("Cần quyền camera để chụp ảnh nguyên liệu. Vui lòng cấp quyền trong cài đặt ứng dụng.")
                        .setPositiveButton("OK", (dialog, which) -> dialog.dismiss())
                        .create()
                        .show();
            }
        });

        // Khởi tạo trình khởi chạy camera để chụp ảnh
        cameraLauncher = registerForActivityResult(new ActivityResultContracts.TakePicturePreview(), result -> {
            if (result != null) {
                // Nếu có ảnh bitmap trả về
                try {
                    capturedBitmap = result;
                    binding.tvEmptyState.setVisibility(View.GONE);
                    binding.cardPreview.setVisibility(View.VISIBLE);
                    binding.buttonsLayout.setVisibility(View.VISIBLE);
                    Glide.with(this).load(capturedBitmap).into(binding.ivImage);

                    Toast.makeText(this, "Đang phân tích ảnh...", Toast.LENGTH_SHORT).show();
                    analyzeImage(capturedBitmap); // Gọi hàm phân tích
                } catch (Exception e) {
                    e.printStackTrace();
                    Toast.makeText(this, "Có lỗi xảy ra khi xử lý ảnh", Toast.LENGTH_SHORT).show();
                }
            } else {
                // Nếu người dùng hủy chụp ảnh
                Toast.makeText(this, "Đã hủy chụp ảnh", Toast.LENGTH_SHORT).show();
            }
        });

        // Thiết lập sự kiện click cho trạng thái trống và nút chụp lại để mở camera
        binding.tvEmptyState.setOnClickListener(v -> openCamera());
        binding.btnRetake.setOnClickListener(v -> openCamera());
    }

    /**
     * Kiểm tra xem ứng dụng đã có quyền truy cập camera hay chưa.
     * Nếu chưa, sẽ tiến hành yêu cầu quyền.
     * @return true nếu đã có quyền, false nếu chưa.
     */
    private boolean checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            // Quyền đã được cấp
            return true;
        } else {
            // Quyền chưa được cấp, tiến hành yêu cầu quyền
            requestPermissionLauncher.launch(Manifest.permission.CAMERA);
            return false;
        }
    }

    /**
     * Mở giao diện camera nếu đã có quyền.
     * Nếu chưa có quyền, sẽ yêu cầu quyền.
     */
    private void openCamera() {
        if (checkCameraPermission()) {
            try {
                cameraLauncher.launch(null);
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(this, "Không thể mở camera", Toast.LENGTH_SHORT).show();
            }
        }
    }

    /**
     * Phân tích hình ảnh đã chụp để nhận dạng nguyên liệu.
     * (Đây là hàm giữ chỗ)
     * @param bitmap ảnh cần phân tích
     */
    private void analyzeImage(Bitmap bitmap) {
        // Logic phân tích ảnh với ML Kit hoặc Gemini sẽ được thêm ở đây
        // Tạm thời hiển thị tên các nguyên liệu mẫu
        binding.tvImageLabel.setText("Phân tích thành công: Cà chua, rau xà lách,...");
    }
}
