package fpt.edu.vn.smartpantrychef;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import fpt.edu.vn.smartpantrychef.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity {

    // Khai báo hằng số cho mã yêu cầu quyền camera
    private static final int CAMERA_PERMISSION_CODE = 100;

    // ViewBinding để truy cập các view trong layout
    private ActivityMainBinding binding;

    // Trình khởi chạy yêu cầu quyền
    private ActivityResultLauncher<String> requestPermissionLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Khởi tạo ViewBinding
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Khởi tạo trình khởi chạy yêu cầu quyền và xử lý kết quả
        requestPermissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
            if (isGranted) {
                // Quyền đã được cấp, thông báo cho người dùng
                Toast.makeText(this, "Quyền camera đã được cấp", Toast.LENGTH_SHORT).show();
                openCamera(); // Mở camera ngay sau khi có quyền
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

        // Thiết lập sự kiện click cho trạng thái trống để yêu cầu quyền và mở camera
        binding.tvEmptyState.setOnClickListener(v -> {
            checkCameraPermission();
        });
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
     */
    private void openCamera() {
        if (checkCameraPermission()) {
            // Logic để mở camera sẽ được triển khai ở đây
            Toast.makeText(this, "Mở camera...", Toast.LENGTH_SHORT).show();
        }
    }
}
