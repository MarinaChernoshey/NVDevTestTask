package com.example.marina.testnvdev;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;
import android.widget.ImageView;
import android.widget.Toast;

import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Random;

public class Region {
    private static final int COUNT_PHOTO_FOR_COLLAGE = 4;

    private Context context;

    private Paint paint;
    private Canvas canvas;
    private Bitmap[] photo;
    private Bitmap collage;

    private JSONArray jsonPlaces;
    private JSONArray jsonPhotos;
    private JSONObject jsonObject;
    private RequestQueue requestQueue;

    private int countPlaces;
    private int numberLoadPhoto;

    private Random random;

    private ArrayList<String> idPlaceWithPhoto;
    String nextPageToken;

    private Uri uri;
    private String urlGetPlaces;
    double ltn;
    double lng;

    public Region(double ltn, double lng, Context context) {
        this.context = context;
        this.ltn = ltn;
        this.lng = lng;

        numberLoadPhoto = 0;
        random = new Random();

        collage = Bitmap.createBitmap(875, 875, Bitmap.Config.RGB_565);
        canvas = new Canvas(collage);
        paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        photo = new Bitmap[COUNT_PHOTO_FOR_COLLAGE];

        idPlaceWithPhoto = new ArrayList<>();
        requestQueue = Volley.newRequestQueue(context);
        nextPageToken = "";

        setUrl();

        requestGetPlaces(urlGetPlaces);
    }

    private void setUrl() {
        urlGetPlaces = context.getString(R.string.url_get_places) +
                context.getString(R.string.url_get_places_location) + ltn + "," + lng +
                context.getString(R.string.url_get_places_params) + nextPageToken +
                context.getString(R.string.url_get_key) +
                context.getString(R.string.google_maps_key);
    }

    private void requestGetPlaces(String url) {
        StringRequest stringRequest = new StringRequest(url, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                searchPlaces(response);
                searchPhoto();
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Toast.makeText(context, context.getString(R.string.error) + error.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        });
        requestQueue.add(stringRequest);
    }

    private void searchPlaces(String response) {
        String photoReference;
        try {

            jsonObject = new JSONObject(response);
            jsonPlaces = jsonObject.getJSONArray(context.getString(R.string.json_places_name_array_result));

            nextPageToken = jsonObject.getString(context.getString(R.string.get_next_page));

            countPlaces = jsonPlaces.length();

            for (int i = 0; i < countPlaces; i++) {
                photoReference = pullOutThePlaceWithPhoto(i);

                if (!photoReference.isEmpty()) {
                    idPlaceWithPhoto.add(photoReference);
                }
            }

        } catch (JSONException e) {
            Toast.makeText(context, context.getString(R.string.not_available),
                    Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }

    private void searchPhoto() {
        String photoReference;
        int rnd;

        if (idPlaceWithPhoto.size() >= COUNT_PHOTO_FOR_COLLAGE) {
            for (int i = 0; i < COUNT_PHOTO_FOR_COLLAGE; ) {

                rnd = (randomNextInt(0, idPlaceWithPhoto.size()));

                photoReference = idPlaceWithPhoto.get(rnd);

                requestGetPhoto(context.getString(R.string.url_get_photos) +
                        context.getString(R.string.url_get_photos_max_width) +
                        context.getString(R.string.url_get_photos_photoreference) + photoReference +
                        context.getString(R.string.url_get_key) +
                        context.getString(R.string.google_maps_key));

                idPlaceWithPhoto.remove(rnd);
                i++;
            }
        } else {
            getNextPage();
        }
    }

    private void getNextPage() {
        if (!nextPageToken.isEmpty()) {
            setUrl();
            searchPlaces(urlGetPlaces);
        } else {
            Toast.makeText(context, context.getString(R.string.message_little_photo),
                    Toast.LENGTH_SHORT).show();
        }
    }

    private void requestGetPhoto(String s) {

        RequestQueue requestQueue = Volley.newRequestQueue(context);

        ImageRequest imageRequest = new ImageRequest(s, new Response.Listener<Bitmap>() {
            @Override
            public void onResponse(Bitmap response) {
                photo[numberLoadPhoto] = processPhoto(response);

                if (numberLoadPhoto == COUNT_PHOTO_FOR_COLLAGE - 1) {
                    createCollage();
                } else {
                    numberLoadPhoto += 1;
                }
            }
        }, 0, 0, ImageView.ScaleType.CENTER_CROP, null, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Toast.makeText(context, context.getString(R.string.error) + error.getMessage(),
                        Toast.LENGTH_SHORT).show();
                error.printStackTrace();

            }
        });
        requestQueue.add(imageRequest);
    }

    private Bitmap processPhoto(Bitmap response) {
        int size = 0;
        int x = 0;
        int y = 0;
        int bitmapWidth = response.getWidth();
        int bitmapHeight = response.getHeight();

        if (bitmapWidth > bitmapHeight) {
            size = bitmapHeight;
            x = (bitmapWidth / 2) - (size / 2);
            y = 0;
        } else if (bitmapWidth <= bitmapHeight) {
            size = bitmapWidth;
            y = (bitmapHeight / 2) - (size / 2);
            x = 0;
        }
        response = Bitmap.createBitmap(response, x, y, size, size);

        return response;
    }

    private void createCollage() {
        MapsActivity.getRegionSearchEqualsTrue(collage);

        int[] x = {25, 450, 25, 450};
        int[] y = {25, 25, 450, 450};

        canvas.drawColor(Color.WHITE);

        for (int i = 0; i < COUNT_PHOTO_FOR_COLLAGE; i++) {
            canvas.drawBitmap(Bitmap.createScaledBitmap(photo[i], 400, 400, true), x[i], y[i], paint);
        }

        saveCollageInFile();
    }

    private void saveCollageInFile() {
        OutputStream os = null;
        try {
            File file = new File(context.getExternalFilesDir(Environment.DIRECTORY_PICTURES),
                    "share_image_" + System.currentTimeMillis() + ".jpeg");
            os = new BufferedOutputStream(new FileOutputStream(file));
            collage.compress(Bitmap.CompressFormat.JPEG, 100, os);
            os.close();
            uri = Uri.fromFile(file);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Uri getUri() {
        return uri;
    }

    private int randomNextInt(int min, int max) {
        return random.nextInt(random.nextInt(max - min) + 1);
    }

    private String pullOutThePlaceWithPhoto(int i) {
        String reference = "";
        try {
            jsonPhotos = new JSONArray(jsonPlaces.getJSONObject(i).getString(context.getString(R.string.json_places_name_array_photos)));
            JSONObject movieObject = jsonPhotos.getJSONObject(0);
            reference = movieObject.getString(context.getString(R.string.json_places_name_object_photoreference));

        } catch (JSONException e) {
            e.printStackTrace();
        }
        return reference;
    }


}
