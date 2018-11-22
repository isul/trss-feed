# TRSS-Feed
Torrent RSS Feed

# 환경
- Download Station
- 기본 포트: 88


# 지원 사이트 및 게시판
- TF
  - tmovie : 영화
  - tdrama : 드라마
  - tent : 예능
  - tv : TV 프로
  - tani : 애니메이션
- TH
  - torrent_movie: 외국영화
  - torrent_kmovie: 한국영화
  - torrent_ent: 예능/오락
  - torrent_docu: 다큐/교양
  - torrent_video: 뮤비/공연
- TW
  - 전체 게시판 검색만 지원

# 주의사항
검색은 30분 단위로 캐시 됩니다.
같은 파라미터로 검색 시 30분간 각 사이트의 검색을 다시시도 하지 않습니다.
Download Station의 RSS 업데이트 간격을 30분 이상으로 설정해주세요.

# 사용방법
__모든 사이트 검색__
```
http://localhost:88/feed?search={검색어}&page={페이지}&maxPage={최대_페이지}
```
- {검색어} : 검색할 단어
- {페이지} : 특정 페이지 부터 검색 (기본값: 1, 생략가능)
- {최대_페이지} : 최대로 검색할 페이지 수 (기본값 : 1, 생략가능)

({페이지} <= {최대페이지})

예) http://localhost:88/feed?search=예능

예) http://localhost:88/feed?search=예능&page=1&maxPage=3


__특정 사이트 및 게시판 검색__
```
http://localhost:88/{사이트}/feed?boards={게시판}&seacrh={검색어}&page={페이지}&maxPage={최대_페이지}
```
- {사이트} : 지원 사이트 목록 (필수)
- {게시판} : 사이트별 지원 게시판 이름 (멀티 게시판 지원, 생략가능)
- {검색어} : 검색할 단어
- {페이지} : 특정 페이지 부터 검색 (기본값: 1, 생략가능)
- {최대_페이지} : 최대로 검색할 페이지 수 (기본값 : 1, 생략가능)

({페이지} <= {최대페이지})

예) http://localhost:88/tf/feed?search=예능

예) http://localhost:88/tf/feed?boards=tent,tv,tani&search=예능

예) http://localhost:88/tf/feed?boards=tent&search=예능&page=1&maxPage=2

# Docker
https://hub.docker.com/r/stkang90/trss/
기본포트 88을 변경하려면 시놀로지 도커의 포트 설정 > 로컬 포트를 변경해주세요.
