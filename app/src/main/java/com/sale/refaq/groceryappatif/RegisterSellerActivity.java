package com.sale.refaq.groceryappatif;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

public class RegisterSellerActivity extends AppCompatActivity implements LocationListener {

    private ImageButton backBtn,gpsBtn;
    private ImageView profileIv;
    private EditText nameEt,phoneEt,countryEt,stateEt,cityEt,addressEt
            ,emailEt,passwordEt,cPasswordEt,shopNameEt,deliveryFeeEt;
    private Button registerBtn;

    //permission constants
    private static final int LOCATION_REQUEST_CODE = 100;
    private static final int CAMERA_REQUEST_CODE = 200;
    private static final int STORAGE_REQUEST_CODE = 300;
    private static final int IMAGE_PICK_GALLERY_CODE = 400;
    private static final int IMAGE_PICK_CAMERA_CODE = 500;
    //permission array
    private String[] locationPermissions;
    private String[] cameraPermissions;
    private String[] storagePermissions;

    //image picked uri
    private Uri image_uri;

    //location service
    private LocationManager locationManager;
    private double latitude = 0.0,longitude=0.0;

    //connect to net
    FirebaseAuth firebaseAuth;
    ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register_seller);

        backBtn = findViewById(R.id.backBtn);
        gpsBtn = findViewById(R.id.gpsBtn);
        profileIv = findViewById(R.id.profileIv);
        nameEt = findViewById(R.id.nameEt);
        phoneEt = findViewById(R.id.phoneEt);
        countryEt = findViewById(R.id.countryEt);
        stateEt = findViewById(R.id.stateEt);
        cityEt = findViewById(R.id.cityEt);
        addressEt = findViewById(R.id.addressEt);
        emailEt = findViewById(R.id.emailEt);
        passwordEt = findViewById(R.id.passwordEt);
        cPasswordEt = findViewById(R.id.cPasswordEt);
        registerBtn = findViewById(R.id.registerBtn);
        shopNameEt = findViewById(R.id.shopNameEt);
        deliveryFeeEt = findViewById(R.id.deliveryFeeEt);

        locationPermissions = new String[]{Manifest.permission.ACCESS_FINE_LOCATION};
        cameraPermissions = new String[]{Manifest.permission.CAMERA,Manifest.permission.WRITE_EXTERNAL_STORAGE};
        storagePermissions = new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE};

        firebaseAuth = FirebaseAuth.getInstance();
        progressDialog = new ProgressDialog(this);
        progressDialog.setTitle("please wait...");
        progressDialog.setCanceledOnTouchOutside(false);

        backBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });
        gpsBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //detect current location
                if (checkLocationPermission())
                {
                    //permission allowed
                    detectLocation();
                }else
                    {
                        //not allowed and request need
                        requestLocationPermission();
                    }
            }
        });
        profileIv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //pick image
                showImagePickDialog();
            }
        });
        registerBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //register user
                inputData();
            }
        });
    }

    private String fullName , shopName,email,password,confirmPassword,city,country,state,address,phone,deliveryFee;
    private void inputData() {
        fullName = nameEt.getText().toString().trim();
        shopName = shopNameEt.getText().toString().trim();
        phone = phoneEt.getText().toString().trim();
        address = addressEt.getText().toString().trim();
        email = emailEt.getText().toString().trim();
        password = passwordEt.getText().toString().trim();
        confirmPassword = cPasswordEt.getText().toString().trim();
        city = cityEt.getText().toString().trim();
        country = countryEt.getText().toString().trim();
        state = stateEt.getText().toString().trim();
        deliveryFee = deliveryFeeEt.getText().toString().trim();

        if (TextUtils.isEmpty(fullName)) {
            Toast.makeText(this, "Enter Name .. ", Toast.LENGTH_SHORT).show();
            return;
        }
        if (TextUtils.isEmpty(shopName)) {
            Toast.makeText(this, "Enter Shop Name .. ", Toast.LENGTH_SHORT).show();
            return;
        }
        if (TextUtils.isEmpty(phone)) {
            Toast.makeText(this, "Enter Phone number .. ", Toast.LENGTH_SHORT).show();
            return;
        }
        if (TextUtils.isEmpty(deliveryFee)) {
            Toast.makeText(this, "Enter Delivery Fee .. ", Toast.LENGTH_SHORT).show();
            return;
        }
        if (latitude==0.0 || longitude==0.0) {
            Toast.makeText(this, "please click gps button to detect location .. ", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(this, "Invalid Email Pattern .. ", Toast.LENGTH_SHORT).show();
            return;
        }
        if (password.length()<6) {
            Toast.makeText(this, "password must be at least 6 characters long .. ", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!password.equals(confirmPassword)) {
            Toast.makeText(this, "password doesn't mach .. ", Toast.LENGTH_SHORT).show();
            return;
        }
        createAccount();
    }
    private void createAccount() {
        progressDialog.setMessage("Creating Account....");
        progressDialog.show();

        firebaseAuth.createUserWithEmailAndPassword(email,password).addOnSuccessListener(new OnSuccessListener<AuthResult>() {
            @Override
            public void onSuccess(AuthResult authResult) {
                saveFirebaseDate();
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                progressDialog.dismiss();
                Toast.makeText(RegisterSellerActivity.this, ""+e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
    private void saveFirebaseDate() {
        progressDialog.setMessage("Saving Account Info...");

        final String timestamp = ""+System.currentTimeMillis();

        if (image_uri==null){
            //without image
            HashMap<String,Object> hashMap = new HashMap<>();
            hashMap.put("uid",""+firebaseAuth.getUid());
            hashMap.put("email",""+email);
            hashMap.put("name",""+fullName);
            hashMap.put("shopName",""+shopName);
            hashMap.put("phone",""+phone);
            hashMap.put("deliveryFee",""+deliveryFee);
            hashMap.put("country",""+country);
            hashMap.put("state",""+state);
            hashMap.put("city",""+city);
            hashMap.put("address",""+address);
            hashMap.put("latitude",""+latitude);
            hashMap.put("longitude",""+longitude);
            hashMap.put("timestamp",""+timestamp);
            hashMap.put("accountType","Seller");
            hashMap.put("online","true");
            hashMap.put("shopOpen","true");
            hashMap.put("profileImage","");

            DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Users");
            ref.child(firebaseAuth.getUid()).setValue(hashMap).addOnSuccessListener(new OnSuccessListener<Void>() {
                @Override
                public void onSuccess(Void aVoid) {
                    progressDialog.dismiss();
                    startActivity(new Intent(getApplicationContext(),MainSellerActivity.class));
                    finish();
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    progressDialog.dismiss();
                    startActivity(new Intent(getApplicationContext(),MainSellerActivity.class));
                    finish();
                }
            });
        }else {
            //with image
            String filePathAndName = "profile_images/"+""+firebaseAuth.getUid();

            StorageReference storageReference = FirebaseStorage.getInstance().getReference(filePathAndName);
            storageReference.putFile(image_uri)
                    .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                            Task<Uri> uriTask = taskSnapshot.getStorage().getDownloadUrl();
                            while(!uriTask.isSuccessful());
                            Uri downloadImageUri = uriTask.getResult();
                            if (uriTask.isSuccessful()) {
                                HashMap<String,Object> hashMap = new HashMap<>();
                                hashMap.put("uid",""+firebaseAuth.getUid());
                                hashMap.put("email",""+email);
                                hashMap.put("name",""+fullName);
                                hashMap.put("shopName",""+shopName);
                                hashMap.put("phone",""+phone);
                                hashMap.put("deliveryFee",""+deliveryFee);
                                hashMap.put("country",""+country);
                                hashMap.put("state",""+state);
                                hashMap.put("city",""+city);
                                hashMap.put("address",""+address);
                                hashMap.put("latitude",""+latitude);
                                hashMap.put("longitude",""+longitude);
                                hashMap.put("timestamp",""+timestamp);
                                hashMap.put("accountType","Seller");
                                hashMap.put("online","true");
                                hashMap.put("shopOpen","true");
                                hashMap.put("profileImage",""+downloadImageUri);

                                DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Users");
                                ref.child(firebaseAuth.getUid()).setValue(hashMap).addOnSuccessListener(new OnSuccessListener<Void>() {
                                    @Override
                                    public void onSuccess(Void aVoid) {
                                        progressDialog.dismiss();
                                        startActivity(new Intent(getApplicationContext(),MainSellerActivity.class));
                                        finish();
                                    }
                                }).addOnFailureListener(new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        progressDialog.dismiss();
                                        startActivity(new Intent(getApplicationContext(),MainSellerActivity.class));
                                        finish();
                                    }
                                });
                            }
                        }
                    }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    progressDialog.dismiss();
                    Toast.makeText(RegisterSellerActivity.this, ""+e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void showImagePickDialog() {
        //options to display
        String[] options = {"Camera","Gallery"};

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Pick Image").setItems(options, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (which == 0)
                {
                    //camera clicked
                    if (checkCameraPermission())
                    {
                        //camera allow
                        pickFromCamera();
                    }else
                        {
                            //not allow
                            requestCameraPermission();
                        }
                }else
                    {
                        //gallery clicked
                        if (checkStoragePermission())
                        {
                            //gallery allow
                            pickFromGallery();
                        }else
                            {
                                //not allow
                                requestStoragePermission();
                            }
                    }
            }
        }).show();
    }
    private void pickFromGallery()
    {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        startActivityForResult(intent,IMAGE_PICK_GALLERY_CODE);
    }
    private void pickFromCamera()
    {
        ContentValues contentValues = new ContentValues();
        contentValues.put(MediaStore.Images.Media.TITLE,"Temp_Image Title");
        contentValues.put(MediaStore.Images.Media.DESCRIPTION,"Temp_Image Description");

        image_uri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,contentValues);

        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        intent.putExtra(MediaStore.EXTRA_OUTPUT,image_uri);
        startActivityForResult(intent,IMAGE_PICK_CAMERA_CODE);
    }
    private void detectLocation() {
        Toast.makeText(this, "please wait ....", Toast.LENGTH_SHORT).show();
        locationManager = (LocationManager)getSystemService(Context.LOCATION_SERVICE);
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,0,0,this);
    }
    @Override
    public void onLocationChanged(Location location) {
        //location detected
        longitude = location.getLongitude();
        latitude = location.getLatitude();

        findAddress();
    }

    private void findAddress() {
        //find address , country , state , city;
        Geocoder geocoder ;
        List<Address> addresses;
        geocoder = new Geocoder(this, Locale.getDefault());
        try {
            addresses = geocoder.getFromLocation(latitude,longitude,1);
            //get data
            String address = addresses.get(0).getAddressLine(0);
            String city = addresses.get(0).getLocality();
            String state = addresses.get(0).getAdminArea();
            String country = addresses.get(0).getCountryName();

            //set data
            cityEt.setText(city);
            addressEt.setText(address);
            stateEt.setText(state);
            countryEt.setText(country);
        }catch (Exception e){}
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

    @Override
    public void onProviderEnabled(String provider) {

    }

    @Override
    public void onProviderDisabled(String provider) {
        //gps turn off
        Toast.makeText(this, "please turn on gps/location ...", Toast.LENGTH_SHORT).show();
    }

    private boolean checkLocationPermission()
    {
        boolean result = ContextCompat.checkSelfPermission(this,Manifest.permission.ACCESS_FINE_LOCATION) == (PackageManager.PERMISSION_GRANTED);
        return  result;
    }
    private void requestLocationPermission()
    {
        ActivityCompat.requestPermissions(this,locationPermissions,LOCATION_REQUEST_CODE);
    }
    private boolean checkStoragePermission()
    {
        boolean result = ContextCompat.checkSelfPermission(this,Manifest.permission.WRITE_EXTERNAL_STORAGE) == (PackageManager.PERMISSION_GRANTED);
        return  result;
    }
    private void requestStoragePermission()
    {
        ActivityCompat.requestPermissions(this,storagePermissions,STORAGE_REQUEST_CODE);
    }
    private boolean checkCameraPermission()
    {
        boolean result = ContextCompat.checkSelfPermission(this,Manifest.permission.CAMERA) == (PackageManager.PERMISSION_GRANTED);
        boolean result1 = ContextCompat.checkSelfPermission(this,Manifest.permission.WRITE_EXTERNAL_STORAGE) == (PackageManager.PERMISSION_GRANTED);
        return  result && result1;
    }
    private void requestCameraPermission()
    {
        ActivityCompat.requestPermissions(this,cameraPermissions,CAMERA_REQUEST_CODE);
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode)
        {
            case LOCATION_REQUEST_CODE :
                {
                    if(grantResults.length>0)
                    {
                        boolean locationAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                        if (locationAccepted)
                        {
                            //permission allowed
                            detectLocation();
                        }else
                            {
                                //permissions denied
                                Toast.makeText(this, "Location Permission is necessary...", Toast.LENGTH_SHORT).show();
                            }
                    }
                }
                break;
            case CAMERA_REQUEST_CODE :
            {
                if(grantResults.length>0)
                {
                    boolean cameraAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                    boolean storageAccepted = grantResults[1] == PackageManager.PERMISSION_GRANTED;
                    if (cameraAccepted && storageAccepted)
                    {
                        //permission allowed
                        pickFromCamera();
                    }else
                    {
                        //permissions denied
                        Toast.makeText(this, "camera Permissions are necessary...", Toast.LENGTH_SHORT).show();
                    }
                }
            }
            break;
            case STORAGE_REQUEST_CODE :
            {
                if(grantResults.length>0)
                {
                    boolean storageAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                    if ( storageAccepted)
                    {
                        //permission allowed
                        pickFromGallery();
                    }else
                    {
                        //permissions denied
                        Toast.makeText(this, "storage Permission is necessary...", Toast.LENGTH_SHORT).show();
                    }
                }
            }
            break;
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (resultCode == RESULT_OK)
        {
            if (requestCode == IMAGE_PICK_GALLERY_CODE)
            {
                image_uri = data.getData();
                profileIv.setImageURI(image_uri);
            }else if (requestCode == IMAGE_PICK_CAMERA_CODE)
            {

                profileIv.setImageURI(image_uri);
            }
        }

        super.onActivityResult(requestCode, resultCode, data);
    }
}
