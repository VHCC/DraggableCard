[![Build Status](https://jenkins.ichenprocin.dsmynas.com/buildStatus/icon?job=DraggableCardCI)](https://jenkins.ichenprocin.dsmynas.com/job/DraggableCardCI/) 
![Maven metadata URL](https://img.shields.io/maven-metadata/v?color=blue&label=Nexus&metadataUrl=https%3A%2F%2Fnexus.ichenprocin.dsmynas.com%2Frepository%2Fvhcc%2Fcom%2Fvhcc%2Flibs%2Fdraggablecard%2Fmaven-metadata.xml&style=plastic)
![GitHub](https://img.shields.io/github/license/vhcc/DraggableCard?style=plastic)

# DraggableCard

# Usage

#### project level

```
allprojects {
    repositories {
        ...
        maven { url 'https://nexus.ichenprocin.dsmynas.com/repository/vhcc/' }
    }
}
```

#### app level

```
implementation 'com.vhcc.libs:draggablecard:0.0.1'
```

# Example

### .xml

```
<com.vhcc.arclayoutlibs.ArcLayout
        android:id="@id/arc_layout"
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        app:arc_axisRadius="@dimen/layout_child_offset_small"
        app:arc_color="@color/scrim"
        app:arc_freeAngle="true"
        app:arc_origin="center"
        app:arc_radius="@dimen/layout_radius_small">

        <com.vhcc.arclayoutlibs.drag.DraggableCardItem
            style="@style/Item.Large"
            android:background="@drawable/light_blue_oval_selector"
            android:text="A"
            app:arc_angle="-75" />

        <com.vhcc.arclayoutlibs.drag.DraggableCardItem
            style="@style/Item.Large"
            android:background="@drawable/light_blue_oval_selector"
            android:text="A"
            app:arc_angle="-45" />

        <com.vhcc.arclayoutlibs.drag.DraggableCardItem
            style="@style/Item.Large"
            android:background="@drawable/cyan_oval_selector"
            android:text="B"
            app:arc_angle="-15" />

        <com.vhcc.arclayoutlibs.drag.DraggableCardItem
            style="@style/Item.Large"
            android:background="@drawable/teal_oval_selector"
            android:text="C"
            app:arc_angle="15" />

        <com.vhcc.arclayoutlibs.drag.DraggableCardItem
            style="@style/Item.Large"
            android:background="@drawable/green_oval_selector"
            android:text="D"
            app:arc_angle="45" />

        <com.vhcc.arclayoutlibs.drag.DraggableCardItem
            style="@style/Item.Large"
            android:background="@drawable/light_green_oval_selector"
            android:text="E"
            app:arc_angle="180" />

        <com.vhcc.arclayoutlibs.drag.DraggableCardItem
            style="@style/Item.Large"
            android:background="@drawable/light_green_oval_selector"
            android:text="F"
            app:arc_angle="145" />

    </com.vhcc.arclayoutlibs.ArcLayout>
```


# refer
 - [ArcLayout](https://github.com/ogaclejapan/ArcLayout)
 - [DragRankSquare](https://github.com/xmuSistone/DragRankSquare)
