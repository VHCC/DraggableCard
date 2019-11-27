[![Build Status](https://jenkins.ichenprocin.dsmynas.com/buildStatus/icon?job=DraggableCardCI)](https://jenkins.ichenprocin.dsmynas.com/job/DraggableCardCI/) 
![Maven metadata URL](https://img.shields.io/maven-metadata/v?color=blue&label=Nexus&metadataUrl=https%3A%2F%2Fnexus.ichenprocin.dsmynas.com%2Frepository%2Fvhcc%2Fcom%2Fvhcc%2Flibs%2Fdraggablecard%2Fmaven-metadata.xml&style=plastic)

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

# refer
 - [ArcLayout](https://github.com/ogaclejapan/ArcLayout)
 - [DragRankSquare](https://github.com/xmuSistone/DragRankSquare)
