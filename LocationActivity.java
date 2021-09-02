package com.qskj.miaoGou.activity;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Point;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.AnimationSet;
import android.view.animation.BounceInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.SearchView;
import android.widget.TextView;

import com.amap.api.location.AMapLocation;
import com.amap.api.location.AMapLocationClient;
import com.amap.api.location.AMapLocationClientOption;
import com.amap.api.location.AMapLocationListener;
import com.amap.api.maps.AMap;
import com.amap.api.maps.AMapUtils;
import com.amap.api.maps.CameraUpdateFactory;
import com.amap.api.maps.LocationSource;
import com.amap.api.maps.MapView;
import com.amap.api.maps.Projection;
import com.amap.api.maps.UiSettings;
import com.amap.api.maps.model.BitmapDescriptorFactory;
import com.amap.api.maps.model.CameraPosition;
import com.amap.api.maps.model.LatLng;
import com.amap.api.maps.model.LatLngBounds;
import com.amap.api.maps.model.Marker;
import com.amap.api.maps.model.MarkerOptions;
import com.amap.api.maps.model.MyLocationStyle;
import com.amap.api.maps.model.animation.Animation;
import com.amap.api.maps.model.animation.TranslateAnimation;
import com.amap.api.services.core.AMapException;
import com.amap.api.services.core.PoiItem;
import com.amap.api.services.geocoder.GeocodeAddress;
import com.amap.api.services.geocoder.GeocodeQuery;
import com.amap.api.services.geocoder.GeocodeResult;
import com.amap.api.services.geocoder.GeocodeSearch;
import com.amap.api.services.geocoder.RegeocodeResult;
import com.amap.api.services.help.Inputtips;
import com.amap.api.services.help.Tip;
import com.amap.api.services.poisearch.PoiResult;
import com.amap.api.services.poisearch.PoiSearch;
import com.qskj.miaoGou.R;
import com.qskj.miaoGou.adapter.PoiListAdapter;
import com.qskj.miaoGou.base.BaseActivity;
import com.qskj.miaoGou.citypicker.CityBean;
import com.qskj.miaoGou.citypicker.CityConfig;
import com.qskj.miaoGou.citypicker.CityPickerView;
import com.qskj.miaoGou.citypicker.DistrictBean;
import com.qskj.miaoGou.citypicker.OnCityItemClickListener;
import com.qskj.miaoGou.citypicker.ProvinceBean;
import com.qskj.miaoGou.utils.LogUtils;
import com.qskj.miaoGou.utils.ToastUtils;
import com.qskj.miaoGou.utils.Utils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;

public class LocationActivity extends BaseActivity implements PoiSearch.OnPoiSearchListener,
        Inputtips.InputtipsListener,SearchView.OnQueryTextListener,AMap.OnMapLoadedListener,
        View.OnClickListener,AMap.OnCameraChangeListener,AMap.InfoWindowAdapter,
        AMap.OnMyLocationChangeListener{
    @BindView(R.id.rl_search)
    RelativeLayout searchLayout;

    @BindView(R.id.sv_address)
    SearchView addressSearchView;

    @BindView(R.id.mapview)
    MapView mapView;

    @BindView(R.id.inputtip_list)
    ListView inputtipListView;

    private PoiListAdapter adapter;
    private AMapLocationClient aMapLocationClient;
    private AMapLocationClientOption mLocationOption;
    private AMap aMap;
    private final CityPickerView  mCityPickerView = new CityPickerView();
    private boolean isFirstLocation = true;
    private Marker screenMarker = null;

    @Override
    protected int setLayout() {
        return R.layout.activity_location;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mapView.onCreate(savedInstanceState);
    }

    @Override
    protected void initStatus() {
        mCityPickerView.init(this);
        searchLayout.setOnClickListener(this);
        addressSearchView.setIconifiedByDefault(false);
        addressSearchView.setSubmitButtonEnabled(true);
        addressSearchView.setQueryHint("请输入您的收货地址");
        addressSearchView.onActionViewExpanded();
        aMap = mapView.getMap();
        UiSettings uiSettings = aMap.getUiSettings();
        uiSettings.setZoomControlsEnabled(false);
        uiSettings.setMyLocationButtonEnabled(true);
        aMap.setOnMapLoadedListener(this);
        aMap.setOnCameraChangeListener(this);
        aMap.setOnMyLocationChangeListener(this);
        aMap.setInfoWindowAdapter(this);
        MyLocationStyle myLocationStyle = new MyLocationStyle();
        myLocationStyle.interval(2000);
        myLocationStyle.myLocationIcon(BitmapDescriptorFactory.fromResource(R.mipmap.map_location));
        myLocationStyle.myLocationType(MyLocationStyle.LOCATION_TYPE_LOCATE);
        myLocationStyle.radiusFillColor(android.R.color.transparent);
        myLocationStyle.strokeColor(android.R.color.transparent);
        myLocationStyle.showMyLocation(false);
        aMap.setMyLocationStyle(myLocationStyle);
        aMap.setMyLocationEnabled(true);
    }

    @Override
    protected boolean setTitleVisible() {
        return false;
    }

    @Override
    protected boolean setIvBackVisible() {
        return false;
    }

    @Override
    public void onPoiSearched(PoiResult poiResult, int rCode) {
        LogUtils.logV("onPoiSearched start!");
        if (rCode == AMapException.CODE_AMAP_SUCCESS) {
            if (poiResult != null ) {
                List<PoiItem> poiItems = poiResult.getPois();
                for(PoiItem poiItem:poiItems){
                    LogUtils.logV("title:"+poiItem.getTitle());
                }
                adapter =new PoiListAdapter(this, poiItems);
                inputtipListView.setAdapter(adapter);
            }
        } else {
            ToastUtils.showerror(this, rCode);
        }
    }

    @Override
    public void onPoiItemSearched(PoiItem poiItem, int rCode) {
        LogUtils.logV("onPoiItemSearched start!");
        if (rCode == AMapException.CODE_AMAP_SUCCESS) {
            List<PoiItem> poiItems = new ArrayList<PoiItem>();
            poiItems.add(poiItem);
            adapter =new PoiListAdapter(this, poiItems);
            inputtipListView.setAdapter(adapter);
        } else {
            ToastUtils.showerror(this, rCode);
        }
    }

    @Override
    public void onGetInputtips(List<Tip> tipList, int rCode) {
        LogUtils.logV("onGetInputtips start!");
        if (rCode == AMapException.CODE_AMAP_SUCCESS) {
            if (tipList != null) {
                List<String> listString = new ArrayList<String>();
                int size = tipList.size();
                for (int i = 0; i < size; i++) {
                    listString.add(tipList.get(i).getName());
                }
            }
        } else {
            ToastUtils.showerror(this, rCode);
        }
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        ToastUtils.showLongToast("您选择的是:"+query);
        return false;
    }

    @Override
    public boolean onQueryTextChange(String newText) {
        LogUtils.logV("onQueryTextChange start!");
        if(TextUtils.isEmpty(newText)){
            inputtipListView.clearTextFilter();
        }else{
            inputtipListView.setFilterText(newText);
        }
        return true;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mapView.onDestroy();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mapView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mapView.onPause();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mapView.onSaveInstanceState(outState);
    }

    @Override
    public void onClick(View view) {
        if(view.getId() == R.id.rl_search){
            CityConfig cityConfig = new CityConfig.Builder()
                    .title("选择城市")
                    .visibleItemsCount(5)
                    .province("北京市")
                    .city("市辖区")
                    .district("东城区")
                    .provinceCyclic(true)
                    .cityCyclic(true)
                    .districtCyclic(true)
                    .setCityWheelType(CityConfig.WheelType.PRO_CITY_DIS)
                    .setCustomItemLayout(R.layout.item_city)//自定义item的布局
                    .setCustomItemTextViewId(R.id.item_city_name_tv)
                    .setShowGAT(true)
                    .build();
            mCityPickerView.setConfig(cityConfig);
            mCityPickerView.setOnCityItemClickListener(new OnCityItemClickListener() {
                @Override
                public void onSelected(ProvinceBean province, CityBean city, DistrictBean district) {
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("选择的结果：\n");
                    stringBuilder.append(province.getName()).append(" ").append(province.getId()).append("\n");
                    stringBuilder.append(city.getName()).append(" ").append(city.getId()).append("\n");
                    stringBuilder.append(district.getName()).append(" ").append(district.getId()).append("\n");
                    LogUtils.logV("message:"+stringBuilder);
                    aMap.moveCamera(CameraUpdateFactory.changeLatLng(new LatLng(39.88288023829594,116.5576263693754)));
//                    final String detailAddress = String.format("%s%s%s",province.getName(),city.getName(),district.getName());
//                     getLocation(detailAddress);
//                   poiSearch(city.getName(),district.getName());
                }
                @Override
                public void onCancel() {
                    ToastUtils.showLongToast( "已取消");
                }
            });
            mCityPickerView.showCityPicker();
        }
    }

    private void poiSearch(String city,String address){
        LogUtils.logV("address:"+address+",city:"+city);
        PoiSearch.Query mPoiSearchQuery = new PoiSearch.Query(address, "", city);
        mPoiSearchQuery.requireSubPois(true);   //true 搜索结果包含POI父子关系; false
        mPoiSearchQuery.setPageSize(20);
        mPoiSearchQuery.setPageNum(0);
        PoiSearch poiSearch = new PoiSearch(this,mPoiSearchQuery);
        poiSearch.setOnPoiSearchListener(this);
        poiSearch.searchPOIAsyn();
    }

    @Override
    public View getInfoWindow(Marker marker) {
        TextView textView = new TextView(this);
        textView.setTextColor(getResources().getColor(android.R.color.black));
        textView.setText(marker.getSnippet());
        return textView;
    }

    @Override
    public View getInfoContents(Marker marker) {
        return null;
    }

    @Override
    public void onCameraChange(CameraPosition cameraPosition) {
        if(isFirstLocation){
            screenMarkerJump(aMap,screenMarker);
            isFirstLocation = false;
        }
    }

    @Override
    public void onCameraChangeFinish(CameraPosition cameraPosition) {
        LogUtils.logV("onCameraChangeFinish--->"+"latitude:"+cameraPosition.target.latitude+",longitude:"+cameraPosition.target.longitude+",zoom:"+cameraPosition.zoom);
        isFirstLocation = true;
        Geocoder geocoder = new Geocoder(this);
        LatLng latLng2=new LatLng(23.025845,113.752532);
        LatLng latLng=new LatLng(23.025845,113.772532);
        float distance = AMapUtils.calculateLineDistance(latLng,latLng2);
        LogUtils.logV("sss===="+Math.round(distance/1000)+" 千米");
        try {
            List<Address> addresses = geocoder.getFromLocation(cameraPosition.target.latitude,cameraPosition.target.longitude,1);
            if(!addresses.isEmpty()){
                Address address = addresses.get(0);
                final String locality = address.getLocality();
                final String detailAddress = address.getAddressLine(0);
//                poiSearch(locality,detailAddress);
//                aMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(cameraPosition.target.latitude,cameraPosition.target.longitude),10));
                screenMarker.setSnippet(detailAddress);
                screenMarker.showInfoWindow();
            }
        } catch (IOException e) {
            LogUtils.logV("exception:"+e.toString());
        }
    }


    public void screenMarkerJump(AMap aMap, Marker screenMarker) {
        if (screenMarker != null) {
            final LatLng latLng = screenMarker.getPosition();
            Point point = aMap.getProjection().toScreenLocation(latLng);
            point.y -= Utils.dip2px(mContext, 10);
            LatLng target = aMap.getProjection().fromScreenLocation(point);
            //使用TranslateAnimation,填写一个需要移动的目标点
            Animation animation = new TranslateAnimation(target);
            animation.setInterpolator(new Interpolator() {
                @Override
                public float getInterpolation(float input) {
                    // 模拟重加速度的interpolator
                    if (input <= 0.5) {
                        return (float) (0.5f - 2 * (0.5 - input) * (0.5 - input));
                    } else {
                        return (float) (0.5f - Math.sqrt((input - 0.5f) * (1.5f - input)));
                    }
                }
            });
            //整个移动所需要的时间
            animation.setDuration(300);
            //设置动画
            screenMarker.setAnimation(animation);
            //开始动画
            screenMarker.startAnimation();
        }
    }

    @Override
    public void onMapLoaded() {
        addMarkerInScreenCenter();
    }

    private void addMarkerInScreenCenter() {
        Point screenPosition = aMap.getProjection().toScreenLocation(aMap.getCameraPosition().target);
        screenMarker = aMap.addMarker(new MarkerOptions()
                .anchor(0.5f,1f)
                .icon(BitmapDescriptorFactory.fromResource(R.mipmap.map_location))
                .draggable(true));
        getInfoWindow(screenMarker);
        screenMarker.setPositionByPixels(screenPosition.x,screenPosition.y);
        screenMarker.setInfoWindowEnable(true);
        screenMarker.setFlat(true);
        screenMarker.setAutoOverturnInfoWindow(true);
    }

    @Override
    public void onMyLocationChange(Location location) {
        LogUtils.logV("location:"+location);
        //aMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(location.getLatitude(),location.getLongitude()),17));
    }

    private void getLocation(String cityName){
        LogUtils.logV("cityName:"+cityName);
        final GeocodeSearch geocodeSearch=new GeocodeSearch(this);
        geocodeSearch.setOnGeocodeSearchListener(new GeocodeSearch.OnGeocodeSearchListener() {
            @Override
            public void onRegeocodeSearched(RegeocodeResult regeocodeResult, int i) {

            }

            @Override
            public void onGeocodeSearched(GeocodeResult geocodeResult, int recode) {
                LogUtils.logV("recode:"+recode);
                if (recode==1000&&geocodeResult!=null){
                    List<GeocodeAddress> geocodeAddresses = geocodeResult.getGeocodeAddressList();
                    if((null != geocodeAddresses)&&(!geocodeAddresses.isEmpty())){
                        GeocodeAddress geocodeAddress = geocodeResult.getGeocodeAddressList().get(0);
                        double latitude = geocodeAddress.getLatLonPoint().getLatitude();//纬度
                        double longitude = geocodeAddress.getLatLonPoint().getLongitude();//经度
                        String decode= geocodeAddress.getAdcode();//区域编码

                        LogUtils.logV("地理编码:"+decode);
                        LogUtils.logV("纬度latitude:"+latitude);
                        LogUtils.logV("经度longititude:"+longitude);
                    }else {
                       ToastUtils.showLongToast("地址名出错");
                    }
                }
            }
        });
        geocodeSearch.getFromLocationNameAsyn(new GeocodeQuery(cityName.trim(),"29"));
    }
}
