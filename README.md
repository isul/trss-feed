# TRSS-Feed
Torrent RSS Feed

# 환경
- Download Station
- 기본 포트: 88


# 지원 사이트 및 게시판
~~- TF (이용불가)~~
  - tmovie: 영화
  - tdrama: 드라마
  - tent: 예능
  - tv: TV 프로
  - tani: 애니메이션
  - tmusic: 음악

~~- TH (이용불가)~~
  - mos: 영화
  - tvs: 국내/해외TV
  - mvs: 음악/영상
  
- TW
  - 전체 게시판 검색만 지원

- TM
  - movie_new: 최신영화
  - movie_old: 이전영화
    kr_ent: 예능/오락
    kr_daq: 시사/요양
    kr_drama: 드라마
    eng_drama: 외국드라마
    ani: 애니
    music: 음악

# 주의사항
검색은 30분 단위로 캐시 됩니다.

같은 파라미터로 검색 시 30분간 각 사이트의 검색을 다시시도 하지 않습니다.

Download Station 의 RSS 업데이트 간격을 30분 이상으로 설정해주세요.

# 사용방법
__모든 사이트 검색__
```
http://localhost:88/rss/feed?search={검색어}&page={페이지}&maxPage={최대_페이지}&prefer={지향단어}
```
- {검색어} : 검색할 단어
- {페이지} : 특정 페이지 부터 검색 (기본값: 1, 생략가능)
- {최대_페이지} : 최대로 검색할 페이지 수 (기본값 : 1, 생략가능)
- {지향단어}: 한 게시물의 여러개의 파일이 있을 경우, 해당 단어를 포함한 파일을 다운로드 (생략가능)

({페이지} <= {최대페이지})

예) http://localhost:88/rss/feed?search=예능

예) http://localhost:88/rss/feed?search=예능&page=1&maxPage=3&prefer=720p


__특정 사이트 및 게시판 검색__
```
http://localhost:88/rss/{사이트}/feed?boards={게시판}&seacrh={검색어}&page={페이지}&maxPage={최대_페이지}&prefer={지향단어}
```
- {사이트} : 지원 사이트 목록 (필수)
- {게시판} : 사이트별 지원 게시판 이름 (멀티 게시판 지원, 생략가능)
- {검색어} : 검색할 단어
- {페이지} : 특정 페이지 부터 검색 (기본값: 1, 생략가능)
- {최대_페이지} : 최대로 검색할 페이지 수 (기본값 : 1, 생략가능)
- {지향단어}: 한 게시물의 여러개의 파일이 있을 경우, 해당 단어를 포함한 파일을 다운로드 (생략가능)

({페이지} <= {최대페이지})

예) http://localhost:88/rss/tf/feed?search=예능

예) http://localhost:88/rss/tf/feed?boards=tent,tv,tani&search=예능

예) http://localhost:88/rss/tf/feed?boards=tent&search=예능&page=1&maxPage=2&prefer=1080p

# Docker
https://hub.docker.com/r/stkang90/trss/
기본포트 88을 변경하려면 시놀로지 도커의 포트 설정 > 로컬 포트를 변경해주세요.
