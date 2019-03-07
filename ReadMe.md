#### 主界面底部工具栏设置

与`CoordinatorLayout`配合使用。

```xml
<android.support.design.bottomappbar.BottomAppBar
        style="@style/Widget.MaterialComponents.BottomAppBar"
        android:id="@+id/main_bottomAppBar"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:layout_gravity="bottom"
        app:navigationIcon="@drawable/ic_menu_white"
        app:backgroundTint="@color/colorPrimary"
        app:fabCradleVerticalOffset="4dp"
        app:fabCradleMargin="@dimen/dimen_8"
        app:fabAlignmentMode="end"
        app:hideOnScroll="true">

    </android.support.design.bottomappbar.BottomAppBar>

    <android.support.design.widget.FloatingActionButton
        android:id="@+id/main_fab"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:src="@drawable/ic_add_white"
        app:fabSize="normal"
        app:layout_anchor="@id/main_bottomAppBar"
        app:layout_anchorGravity="top"/>
```

#### 取消顶部活动栏

在`styles.xml`的主题中加一句

```xml
<!-- 这两句必须同时写，不然会出错 -->
<item name="windowActionBar">false</item>
<item name="windowNoTitle">true</item>
```

