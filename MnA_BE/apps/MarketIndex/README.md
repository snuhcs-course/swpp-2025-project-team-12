# marketindex

## stockindex

KOSPI과 KOSDAQ 지수를 Local환경 혹은 EC2 환경에서 json에 저장합니다.

### 1. Initial setup

지난 365일동안의 주가지수를 /stockindex에 저장합니다. MarketIndex 디렉토리에서 아래 명령어를 실행합니다.
` python3 -c "from stockindex_manager import setup_initial_data; setup_initial_data()" `

### 2. Add to cron

매일 한국장 마감 후 (오후 3:35) 업데이트를 합니다. crontab 명령어는 아무 디렉토리에서 실행해도 상관 없습니다.

`crontab -e`

Add this line, where PATH_TO_FOLDER is the full path to the folder stockindex_manager.py is at:

`35 15 * * 1-5 cd PATH_TO_FOLDER && python3 -c "from stockindex_manager import daily_update; daily_update()"`

Example:

`35 15 * * 1-5 cd ~/swpp/swpp-2025-project-team-12/MnA_BE/apps/MarketIndex && python3 -c "from stockindex_manager import daily_update; daily_update()"`