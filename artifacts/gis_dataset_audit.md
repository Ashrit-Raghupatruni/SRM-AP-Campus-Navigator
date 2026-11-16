# GIS Dataset Profiling & Audit Report

**Source Directory**: `C:\Users\ashri\Downloads\Remote Sensing and GIS\Blocks`  
**Total Valid Datasets**: 19

## Dataset Overview Table

| Filename | Features | Geom Type | CRS (EPSG) | Fields | Missing Sidecars |
| :--- | :---: | :---: | :---: | :--- | :--- |
| `LEVEL 1.shp` | 9 | Point | UNKNOWN | `[]` | .dbf, .prj, .cpg |
| `LEVEL 2.shp` | 34 | Point | UNKNOWN | `[]` | .dbf, .prj, .cpg |
| `LEVEL 3.shp` | 3 | Point | UNKNOWN | `[]` | .dbf, .prj, .cpg |
| `LEVEL 4.shp` | 16 | Point | UNKNOWN | `[]` | .dbf, .prj, .cpg |
| `LEVEL 5.shp` | 2 | Point | UNKNOWN | `[]` | .dbf, .prj, .cpg |
| `Sr1_block.shp` | 219 | Point | 4326 | `['id', 'name', 'room_num']` | None |
| `VBlock.shp` | 44 | Point | 4326 | `['id', 'Name', 'Room_No']` | None |
| `X-LAB_1to3.shp` | 42 | Point | 4326 | `['id', 'Name', 'Room_No']` | None |
| `buildings_srmap.shp` | 21 | Polygon | 4326 | `['Id', 'Building N']` | None |
| `cv_block.shp` | 212 | Point | 4326 | `['id', 'Name', 'Room_no']` | None |
| `cv_block.shp` | 212 | Point | 4326 | `['id', 'Name', 'Room_no']` | None |
| `LEVEL 1.shp` | 9 | Point | UNKNOWN | `[]` | .dbf, .prj, .cpg |
| `LEVEL 2.shp` | 34 | Point | UNKNOWN | `[]` | .dbf, .prj, .cpg |
| `LEVEL 3.shp` | 3 | Point | UNKNOWN | `[]` | .dbf, .prj, .cpg |
| `LEVEL 4.shp` | 16 | Point | UNKNOWN | `[]` | .dbf, .prj, .cpg |
| `LEVEL 5.shp` | 2 | Point | UNKNOWN | `[]` | .dbf, .prj, .cpg |
| `Sr1_block.shp` | 219 | Point | 4326 | `['id', 'name', 'room_num']` | None |
| `VBlock.shp` | 44 | Point | 4326 | `['id', 'Name', 'Room_No']` | None |
| `X-LAB_1to3.shp` | 42 | Point | 4326 | `['id', 'Name', 'Room_No']` | None |

## Detailed Findings

### `LEVEL 1.shp`
- **Path**: `ALL\LEVEL 1.shp`
- **Count**: 9 point features
- **Bounds [minX, maxX, minY, maxY]**: `(80.50808082134387, 80.50844934853508, 16.46458046361783, 16.465086252629686)`
- **Null Rooms**: 9
- **Duplicate Room Codes**: `{}`
- **Sample Records**:
```json
[
  {
    "id": 0,
    "attributes": {},
    "point": [
      80.50808082134387,
      16.464838349462692
    ]
  },
  {
    "id": 1,
    "attributes": {},
    "point": [
      80.50824303985918,
      16.464839181352513
    ]
  },
  {
    "id": 2,
    "attributes": {},
    "point": [
      80.50844851664526,
      16.465026356562493
    ]
  }
]
```

### `LEVEL 2.shp`
- **Path**: `ALL\LEVEL 2.shp`
- **Count**: 34 point features
- **Bounds [minX, maxX, minY, maxY]**: `(80.50808082134387, 80.50844602097578, 16.464560082317185, 16.465087500464417)`
- **Null Rooms**: 34
- **Duplicate Room Codes**: `{}`
- **Sample Records**:
```json
[
  {
    "id": 0,
    "attributes": {},
    "point": [
      80.50808082134387,
      16.46508583668477
    ]
  },
  {
    "id": 1,
    "attributes": {},
    "point": [
      80.50809080402173,
      16.465086668574596
    ]
  },
  {
    "id": 2,
    "attributes": {},
    "point": [
      80.50811160126729,
      16.46508583668477
    ]
  }
]
```

### `LEVEL 3.shp`
- **Path**: `ALL\LEVEL 3.shp`
- **Count**: 3 point features
- **Bounds [minX, maxX, minY, maxY]**: `(80.50825635009629, 80.50844518908592, 16.46460791598196, 16.46504798569787)`
- **Null Rooms**: 3
- **Duplicate Room Codes**: `{}`
- **Sample Records**:
```json
[
  {
    "id": 0,
    "attributes": {},
    "point": [
      80.50844518908592,
      16.464840013242338
    ]
  },
  {
    "id": 1,
    "attributes": {},
    "point": [
      80.50825635009629,
      16.46504798569787
    ]
  },
  {
    "id": 2,
    "attributes": {},
    "point": [
      80.50827465167238,
      16.46460791598196
    ]
  }
]
```

### `LEVEL 4.shp`
- **Path**: `ALL\LEVEL 4.shp`
- **Count**: 16 point features
- **Bounds [minX, maxX, minY, maxY]**: `(80.50809329969115, 80.50836699144263, 16.464590446295695, 16.46506711916378)`
- **Null Rooms**: 16
- **Duplicate Room Codes**: `{}`
- **Sample Records**:
```json
[
  {
    "id": 0,
    "attributes": {},
    "point": [
      80.50809329969115,
      16.46506711916378
    ]
  },
  {
    "id": 1,
    "attributes": {},
    "point": [
      80.50820310914767,
      16.46506379160449
    ]
  },
  {
    "id": 2,
    "attributes": {},
    "point": [
      80.50835700876476,
      16.464988089630676
    ]
  }
]
```

### `LEVEL 5.shp`
- **Path**: `ALL\LEVEL 5.shp`
- **Count**: 2 point features
- **Bounds [minX, maxX, minY, maxY]**: `(80.50824470363878, 80.5082679965538, 16.464588782516053, 16.46505214514698)`
- **Null Rooms**: 2
- **Duplicate Room Codes**: `{}`
- **Sample Records**:
```json
[
  {
    "id": 0,
    "attributes": {},
    "point": [
      80.50824470363878,
      16.46505214514698
    ]
  },
  {
    "id": 1,
    "attributes": {},
    "point": [
      80.5082679965538,
      16.464588782516053
    ]
  }
]
```

### `Sr1_block.shp`
- **Path**: `ALL\Sr1_block.shp`
- **Count**: 219 point features
- **Bounds [minX, maxX, minY, maxY]**: `(80.5063754241783, 80.50688750778542, 16.4619990720216, 16.462987119795674)`
- **Null Rooms**: 0
- **Duplicate Room Codes**: `{'S402': 2}`
- **Sample Records**:
```json
[
  {
    "id": 0,
    "attributes": {
      "id": 1,
      "name": "SR_block",
      "room_num": "s203"
    },
    "point": [
      80.50686758146739,
      16.462006565806472
    ]
  },
  {
    "id": 1,
    "attributes": {
      "id": 2,
      "name": "SR_block",
      "room_num": "s202"
    },
    "point": [
      80.50688290940434,
      16.46204641844255
    ]
  },
  {
    "id": 2,
    "attributes": {
      "id": 3,
      "name": "SR_block",
      "room_num": "s201"
    },
    "point": [
      80.50688597499173,
      16.462303927783353
    ]
  }
]
```

### `VBlock.shp`
- **Path**: `ALL\VBlock.shp`
- **Count**: 44 point features
- **Bounds [minX, maxX, minY, maxY]**: `(80.50725689736214, 80.50786526744801, 16.464583993151265, 16.46506471953522)`
- **Null Rooms**: 2
- **Duplicate Room Codes**: `{}`
- **Sample Records**:
```json
[
  {
    "id": 0,
    "attributes": {
      "id": 1,
      "Name": "V_Block",
      "Room_No": "V - 101"
    },
    "point": [
      80.50786526744801,
      16.46461238250465
    ]
  },
  {
    "id": 1,
    "attributes": {
      "id": 2,
      "Name": "Quantum_Open_Lab",
      "Room_No": null
    },
    "point": [
      80.50729748038036,
      16.464606704633972
    ]
  },
  {
    "id": 2,
    "attributes": {
      "id": 3,
      "Name": "Quantum_Lab_Close",
      "Room_No": null
    },
    "point": [
      80.50768357558637,
      16.46460859725753
    ]
  }
]
```

### `X-LAB_1to3.shp`
- **Path**: `ALL\X-LAB_1to3.shp`
- **Count**: 42 point features
- **Bounds [minX, maxX, minY, maxY]**: `(80.50723844061339, 80.50757586683044, 16.46350034401922, 16.464182997696906)`
- **Null Rooms**: 0
- **Duplicate Room Codes**: `{'X-113': 2, 'AUD-1': 3, 'X-201': 2, 'LIFT': 2, 'X-204': 2, 'X-205': 2}`
- **Sample Records**:
```json
[
  {
    "id": 0,
    "attributes": {
      "id": 1,
      "Name": "X-Lab",
      "Room_No": "X-101"
    },
    "point": [
      80.50748876177157,
      16.463520099407294
    ]
  },
  {
    "id": 1,
    "attributes": {
      "id": 2,
      "Name": "X-Lab",
      "Room_No": "X-102"
    },
    "point": [
      80.50740696562445,
      16.46351858466383
    ]
  },
  {
    "id": 2,
    "attributes": {
      "id": 3,
      "Name": "X-Lab",
      "Room_No": "X-103"
    },
    "point": [
      80.5073100220427,
      16.4635155551769
    ]
  }
]
```

### `buildings_srmap.shp`
- **Path**: `ALL\buildings_srmap.shp`
- **Count**: 21 point features
- **Bounds [minX, maxX, minY, maxY]**: `(80.5052733, 80.5084925, 16.4616739, 16.4664846)`
- **Null Rooms**: 21
- **Duplicate Room Codes**: `{}`
- **Sample Records**:
```json
[
  {
    "id": 0,
    "attributes": {
      "Id": 1,
      "Building N": "Narmadha"
    },
    "point": null
  },
  {
    "id": 1,
    "attributes": {
      "Id": 2,
      "Building N": "Yamuna"
    },
    "point": null
  },
  {
    "id": 2,
    "attributes": {
      "Id": 3,
      "Building N": "Yamuna"
    },
    "point": null
  }
]
```

### `cv_block.shp`
- **Path**: `ALL\cv_block.shp`
- **Count**: 212 point features
- **Bounds [minX, maxX, minY, maxY]**: `(80.50537423925957, 80.5060529864629, 16.46168282575819, 16.461971845408517)`
- **Null Rooms**: 0
- **Duplicate Room Codes**: `{'207': 2, 'c16_2': 2, 'c13_7': 2, '809': 3}`
- **Sample Records**:
```json
[
  {
    "id": 0,
    "attributes": {
      "id": 1,
      "Name": "cv",
      "Room_no": "210"
    },
    "point": [
      80.5059614622246,
      16.46194854915985
    ]
  },
  {
    "id": 1,
    "attributes": {
      "id": 2,
      "Name": "cv",
      "Room_no": "209"
    },
    "point": [
      80.50589370572864,
      16.46194854915985
    ]
  },
  {
    "id": 2,
    "attributes": {
      "id": 3,
      "Name": "cv",
      "Room_no": "207"
    },
    "point": [
      80.50582800245982,
      16.4619464959327
    ]
  }
]
```

### `cv_block.shp`
- **Path**: `CV_block\cv_block\cv_block.shp`
- **Count**: 212 point features
- **Bounds [minX, maxX, minY, maxY]**: `(80.50537423925957, 80.5060529864629, 16.46168282575819, 16.461971845408517)`
- **Null Rooms**: 0
- **Duplicate Room Codes**: `{'207': 2, 'c16_2': 2, 'c13_7': 2, '809': 3}`
- **Sample Records**:
```json
[
  {
    "id": 0,
    "attributes": {
      "id": 1,
      "Name": "cv",
      "Room_no": "210"
    },
    "point": [
      80.5059614622246,
      16.46194854915985
    ]
  },
  {
    "id": 1,
    "attributes": {
      "id": 2,
      "Name": "cv",
      "Room_no": "209"
    },
    "point": [
      80.50589370572864,
      16.46194854915985
    ]
  },
  {
    "id": 2,
    "attributes": {
      "id": 3,
      "Name": "cv",
      "Room_no": "207"
    },
    "point": [
      80.50582800245982,
      16.4619464959327
    ]
  }
]
```

### `LEVEL 1.shp`
- **Path**: `HOMI J BHABHA\HOMI J BHABHA\LEVEL 1.shp`
- **Count**: 9 point features
- **Bounds [minX, maxX, minY, maxY]**: `(80.50808082134387, 80.50844934853508, 16.46458046361783, 16.465086252629686)`
- **Null Rooms**: 9
- **Duplicate Room Codes**: `{}`
- **Sample Records**:
```json
[
  {
    "id": 0,
    "attributes": {},
    "point": [
      80.50808082134387,
      16.464838349462692
    ]
  },
  {
    "id": 1,
    "attributes": {},
    "point": [
      80.50824303985918,
      16.464839181352513
    ]
  },
  {
    "id": 2,
    "attributes": {},
    "point": [
      80.50844851664526,
      16.465026356562493
    ]
  }
]
```

### `LEVEL 2.shp`
- **Path**: `HOMI J BHABHA\HOMI J BHABHA\LEVEL 2.shp`
- **Count**: 34 point features
- **Bounds [minX, maxX, minY, maxY]**: `(80.50808082134387, 80.50844602097578, 16.464560082317185, 16.465087500464417)`
- **Null Rooms**: 34
- **Duplicate Room Codes**: `{}`
- **Sample Records**:
```json
[
  {
    "id": 0,
    "attributes": {},
    "point": [
      80.50808082134387,
      16.46508583668477
    ]
  },
  {
    "id": 1,
    "attributes": {},
    "point": [
      80.50809080402173,
      16.465086668574596
    ]
  },
  {
    "id": 2,
    "attributes": {},
    "point": [
      80.50811160126729,
      16.46508583668477
    ]
  }
]
```

### `LEVEL 3.shp`
- **Path**: `HOMI J BHABHA\HOMI J BHABHA\LEVEL 3.shp`
- **Count**: 3 point features
- **Bounds [minX, maxX, minY, maxY]**: `(80.50825635009629, 80.50844518908592, 16.46460791598196, 16.46504798569787)`
- **Null Rooms**: 3
- **Duplicate Room Codes**: `{}`
- **Sample Records**:
```json
[
  {
    "id": 0,
    "attributes": {},
    "point": [
      80.50844518908592,
      16.464840013242338
    ]
  },
  {
    "id": 1,
    "attributes": {},
    "point": [
      80.50825635009629,
      16.46504798569787
    ]
  },
  {
    "id": 2,
    "attributes": {},
    "point": [
      80.50827465167238,
      16.46460791598196
    ]
  }
]
```

### `LEVEL 4.shp`
- **Path**: `HOMI J BHABHA\HOMI J BHABHA\LEVEL 4.shp`
- **Count**: 16 point features
- **Bounds [minX, maxX, minY, maxY]**: `(80.50809329969115, 80.50836699144263, 16.464590446295695, 16.46506711916378)`
- **Null Rooms**: 16
- **Duplicate Room Codes**: `{}`
- **Sample Records**:
```json
[
  {
    "id": 0,
    "attributes": {},
    "point": [
      80.50809329969115,
      16.46506711916378
    ]
  },
  {
    "id": 1,
    "attributes": {},
    "point": [
      80.50820310914767,
      16.46506379160449
    ]
  },
  {
    "id": 2,
    "attributes": {},
    "point": [
      80.50835700876476,
      16.464988089630676
    ]
  }
]
```

### `LEVEL 5.shp`
- **Path**: `HOMI J BHABHA\HOMI J BHABHA\LEVEL 5.shp`
- **Count**: 2 point features
- **Bounds [minX, maxX, minY, maxY]**: `(80.50824470363878, 80.5082679965538, 16.464588782516053, 16.46505214514698)`
- **Null Rooms**: 2
- **Duplicate Room Codes**: `{}`
- **Sample Records**:
```json
[
  {
    "id": 0,
    "attributes": {},
    "point": [
      80.50824470363878,
      16.46505214514698
    ]
  },
  {
    "id": 1,
    "attributes": {},
    "point": [
      80.5082679965538,
      16.464588782516053
    ]
  }
]
```

### `Sr1_block.shp`
- **Path**: `OE1\OE1\Sr1_block.shp`
- **Count**: 219 point features
- **Bounds [minX, maxX, minY, maxY]**: `(80.5063754241783, 80.50688750778542, 16.4619990720216, 16.462987119795674)`
- **Null Rooms**: 0
- **Duplicate Room Codes**: `{'S402': 2}`
- **Sample Records**:
```json
[
  {
    "id": 0,
    "attributes": {
      "id": 1,
      "name": "SR_block",
      "room_num": "s203"
    },
    "point": [
      80.50686758146739,
      16.462006565806472
    ]
  },
  {
    "id": 1,
    "attributes": {
      "id": 2,
      "name": "SR_block",
      "room_num": "s202"
    },
    "point": [
      80.50688290940434,
      16.46204641844255
    ]
  },
  {
    "id": 2,
    "attributes": {
      "id": 3,
      "name": "SR_block",
      "room_num": "s201"
    },
    "point": [
      80.50688597499173,
      16.462303927783353
    ]
  }
]
```

### `VBlock.shp`
- **Path**: `VBlock\VBlock\VBlock.shp`
- **Count**: 44 point features
- **Bounds [minX, maxX, minY, maxY]**: `(80.50725689736214, 80.50786526744801, 16.464583993151265, 16.46506471953522)`
- **Null Rooms**: 2
- **Duplicate Room Codes**: `{}`
- **Sample Records**:
```json
[
  {
    "id": 0,
    "attributes": {
      "id": 1,
      "Name": "V_Block",
      "Room_No": "V - 101"
    },
    "point": [
      80.50786526744801,
      16.46461238250465
    ]
  },
  {
    "id": 1,
    "attributes": {
      "id": 2,
      "Name": "Quantum_Open_Lab",
      "Room_No": null
    },
    "point": [
      80.50729748038036,
      16.464606704633972
    ]
  },
  {
    "id": 2,
    "attributes": {
      "id": 3,
      "Name": "Quantum_Lab_Close",
      "Room_No": null
    },
    "point": [
      80.50768357558637,
      16.46460859725753
    ]
  }
]
```

### `X-LAB_1to3.shp`
- **Path**: `x-lab_1to3\x-lab_1to3\X-LAB_1to3.shp`
- **Count**: 42 point features
- **Bounds [minX, maxX, minY, maxY]**: `(80.50723844061339, 80.50757586683044, 16.46350034401922, 16.464182997696906)`
- **Null Rooms**: 0
- **Duplicate Room Codes**: `{'X-113': 2, 'AUD-1': 3, 'X-201': 2, 'LIFT': 2, 'X-204': 2, 'X-205': 2}`
- **Sample Records**:
```json
[
  {
    "id": 0,
    "attributes": {
      "id": 1,
      "Name": "X-Lab",
      "Room_No": "X-101"
    },
    "point": [
      80.50748876177157,
      16.463520099407294
    ]
  },
  {
    "id": 1,
    "attributes": {
      "id": 2,
      "Name": "X-Lab",
      "Room_No": "X-102"
    },
    "point": [
      80.50740696562445,
      16.46351858466383
    ]
  },
  {
    "id": 2,
    "attributes": {
      "id": 3,
      "Name": "X-Lab",
      "Room_No": "X-103"
    },
    "point": [
      80.5073100220427,
      16.4635155551769
    ]
  }
]
```

