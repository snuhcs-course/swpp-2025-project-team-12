"""
Korean stock market data manager (KOSPI & KOSDAQ only).
Fetches data at 3:35 PM KST after market close.
Stores data in S3 with partitioning AND local JSON files.
Provides comprehensive data query methods.

UAT Support: When UAT_DATE environment variable is set,
returns data on or before that date.
"""

import json
import os
from datetime import datetime, timedelta
from typing import Dict, List, Optional
import yfinance as yf
import pandas as pd
from pathlib import Path
from S3.client_factory import S3ClientFactory
from botocore.exceptions import ClientError

# UAT 날짜 유틸리티 import
try:
    from utils.uat_date import get_uat_date
except ImportError:

    def get_uat_date():
        return None

# Get the directory where this script is located
BASE_DIR = Path(__file__).resolve().parent

S3_BUCKET_NAME = os.getenv("FINANCE_BUCKET_NAME")


class StockindexManager:
    """
    Manages KOSPI and KOSDAQ data in both S3 and local storage.
    Designed to run at 3:35 PM KST after market close.
    Provides comprehensive query and analysis methods.

    UAT Mode: When UAT_DATE env is set, queries return data
    on or before that date instead of the latest.
    """

    def __init__(self, data_dir_name: str = "stockindex", use_s3: bool = True):
        """
        Initialize the manager for Korean markets.

        Args:
            data_dir_name: Local directory for JSON files (default: "stockindex")
            use_s3: Whether to use S3 storage (default: True)
        """
        self.data_dir = BASE_DIR / data_dir_name
        self.data_dir.mkdir(exist_ok=True)

        # S3 클라이언트 초기화
        self.use_s3 = use_s3
        if self.use_s3:
            try:
                self.s3_client = S3ClientFactory().create("finance")
            except Exception as e:
                print(f"⚠️  S3 초기화 실패: {e}")
                self.use_s3 = False

        # Korean indices only
        self.indices = {"KOSPI": "^KS11", "KOSDAQ": "^KQ11"}

        self.max_days = 365  # Keep 1 year of history
        self.market_close_time = "15:30"  # Korean market closes at 3:30 PM

    def _get_target_date(self, data: Dict) -> Optional[str]:
        """
        Get target date based on UAT mode.

        UAT mode: returns the most recent date on or before UAT_DATE
        Normal mode: returns the latest date

        Args:
            data: Dictionary with date strings as keys

        Returns:
            Target date string or None if no valid date found
        """
        if not data:
            return None

        uat_date = get_uat_date()

        if uat_date:
            # UAT 모드: 해당 날짜 이전의 가장 최근 데이터
            valid_dates = [d for d in data.keys() if d <= uat_date]
            if valid_dates:
                return max(valid_dates)
            return None
        else:
            # 일반 모드: 최신 날짜
            return max(data.keys())

    def _get_local_file_path(self, index_type: str) -> Path:
        """Get the local JSON file path for an index."""
        return self.data_dir / f"{index_type}.json"

    def _get_s3_key(self, index_type: str, date_obj: datetime) -> str:
        """Get S3 key with partitioning."""
        year = date_obj.strftime("%Y")
        month = date_obj.strftime("%-m")  # 0 없이
        day = date_obj.strftime("%-d")  # 0 없이
        return f"stock-indices/year={year}/month={month}/day={day}/{index_type}.json"

    def _load_from_s3(self, index_type: str, date_obj: datetime) -> Optional[Dict]:
        """Load data from S3."""
        if not self.use_s3:
            return None

        try:
            s3_key = self._get_s3_key(index_type, date_obj)
            response = self.s3_client.get_object(Bucket=S3_BUCKET_NAME, Key=s3_key)
            data = json.loads(response["Body"].read().decode("utf-8"))
            return data
        except ClientError as e:
            if e.response["Error"]["Code"] == "NoSuchKey":
                return None
            print(f"[S3] Error loading {index_type}: {e}")
            return None
        except Exception as e:
            print(f"[S3] Error: {e}")
            return None

    def _save_to_s3(self, index_type: str, data: Dict, date_obj: datetime) -> bool:
        """Save data to S3."""
        if not self.use_s3:
            return False

        try:
            s3_key = self._get_s3_key(index_type, date_obj)

            # Convert to JSON string
            json_data = json.dumps(data, ensure_ascii=False, indent=2)

            # Upload to S3
            self.s3_client.put_object(
                Bucket=S3_BUCKET_NAME,
                Key=s3_key,
                Body=json_data.encode("utf-8"),
                ContentType="application/json",
            )

            print(f"[S3] ✓ Uploaded: s3://{S3_BUCKET_NAME}/{s3_key}")
            return True
        except Exception as e:
            print(f"[S3] ✗ Upload failed for {index_type}: {e}")
            return False

    def _load_local_data(self, index_type: str) -> Dict:
        """Load data from local JSON file."""
        file_path = self._get_local_file_path(index_type)

        if file_path.exists():
            with open(file_path, "r", encoding="utf-8") as f:
                return json.load(f)
        return {}

    def _save_local_data(self, index_type: str, data: Dict):
        """Save data to local JSON file."""
        file_path = self._get_local_file_path(index_type)

        # Sort by date and keep only recent data
        sorted_dates = sorted(data.keys(), reverse=True)[: self.max_days]
        cleaned_data = {date: data[date] for date in sorted_dates}

        with open(file_path, "w", encoding="utf-8") as f:
            json.dump(cleaned_data, f, ensure_ascii=False, indent=2, sort_keys=True)

    def fetch_daily(self):
        """
        Fetch today's closing prices for KOSPI and KOSDAQ.
        Save to both S3 and local storage.
        Run this at 3:35 PM KST (after market close).
        """
        current_time = datetime.now()
        print(f"\n{'='*50}")
        print(f"한국 주식시장 종가 업데이트")
        print(f"시간: {current_time.strftime('%Y-%m-%d %H:%M:%S')}")
        print("=" * 50)

        results = {}

        for name, symbol in self.indices.items():
            try:
                # Fetch recent data
                ticker = yf.Ticker(symbol)
                hist = ticker.history(period="5d")

                if len(hist) < 1:
                    print(f"⚠️  {name}: 데이터 없음")
                    continue

                # Get latest data
                latest_date = hist.index[-1]
                latest_row = hist.iloc[-1]
                date_str = latest_date.strftime("%Y-%m-%d")
                date_obj = latest_date.to_pydatetime()

                # Load existing local data for duplicate check
                existing_data = self._load_local_data(name)

                # Check if already updated today (duplicate prevention)
                # But still upload to S3 if not there
                skip_local = False
                if date_str in existing_data:
                    existing_close = existing_data[date_str]["close"]
                    if abs(existing_close - float(latest_row["Close"])) < 0.01:
                        # Check if S3 also has it
                        s3_data = self._load_from_s3(name, date_obj) if self.use_s3 else None
                        if s3_data is not None:
                            print(f"⏭️  {name}: 이미 최신 데이터 (로컬 + S3)")
                            continue
                        else:
                            print(f"📤 {name}: 로컬에 있음, S3에 업로드 필요")
                            skip_local = True

                # Calculate change from previous day
                if len(hist) >= 2:
                    prev_close = float(hist["Close"].iloc[-2])
                else:
                    # Get from existing data
                    sorted_dates = sorted(existing_data.keys(), reverse=True)
                    if sorted_dates and sorted_dates[0] != date_str:
                        prev_close = existing_data[sorted_dates[0]]["close"]
                    else:
                        prev_close = float(latest_row["Close"])

                change_amount = float(latest_row["Close"]) - prev_close
                change_percent = (change_amount / prev_close) * 100 if prev_close != 0 else 0

                # Create record (with index field for S3)
                record = {
                    "index": name,
                    "date": date_str,
                    "open": (
                        round(float(latest_row["Open"]), 2)
                        if pd.notna(latest_row["Open"])
                        else None
                    ),
                    "high": (
                        round(float(latest_row["High"]), 2)
                        if pd.notna(latest_row["High"])
                        else None
                    ),
                    "low": (
                        round(float(latest_row["Low"]), 2) if pd.notna(latest_row["Low"]) else None
                    ),
                    "close": round(float(latest_row["Close"]), 2),
                    "change_amount": round(change_amount, 2),
                    "change_percent": round(change_percent, 2),
                    "volume": int(latest_row["Volume"]) if pd.notna(latest_row["Volume"]) else None,
                    "fetched_at": current_time.isoformat(),
                }

                # Save to S3
                if self.use_s3:
                    self._save_to_s3(name, record, date_obj)

                # Save to local storage (skip if already there)
                if not skip_local:
                    existing_data[date_str] = record
                    self._save_local_data(name, existing_data)

                # Display with emoji
                emoji = "📈" if change_percent >= 0 else "📉"
                print(f"{emoji} {name}: {record['close']:,}원 ({change_percent:+.2f}%)")
                results[name] = record

            except Exception as e:
                print(f"❌ {name} 실패: {e}")
                results[name] = {"error": str(e)}

        print(f"\n✅ 업데이트 완료!")
        return results

    def fetch_historical(self, days: int = 365):
        """
        Fetch historical data for Korean markets and save to both S3 and local.
        Run this ONCE to initialize data.

        Args:
            days: Number of days to fetch (default: 365)
        """
        print(f"\n{'='*50}")
        print(f"한국 주식시장 {days}일 데이터 다운로드 및 저장")
        print(f"Fetching {days} days of Korean market data")
        print("=" * 50)

        results = {}

        for name, symbol in self.indices.items():
            print(f"\n📊 {name} 처리중...")

            try:
                # Fetch from Yahoo Finance
                ticker = yf.Ticker(symbol)
                end_date = datetime.now()
                start_date = end_date - timedelta(days=days + 30)

                print(f"   Yahoo Finance에서 다운로드...")
                hist = ticker.history(start=start_date, end=end_date)

                if hist.empty:
                    print(f"   ⚠️  {name} 데이터 없음")
                    continue

                # Load existing local data
                existing_data = self._load_local_data(name)

                new_count = 0
                updated_count = 0
                s3_uploaded_count = 0
                cutoff_date = (datetime.now() - timedelta(days=days)).date()

                for i, (date, row) in enumerate(hist.iterrows()):
                    # Skip old dates
                    if date.date() < cutoff_date:
                        continue

                    date_str = date.strftime("%Y-%m-%d")
                    date_obj = date.to_pydatetime()

                    # Calculate change
                    if i > 0:
                        prev_close = hist["Close"].iloc[i - 1]
                        change_amount = row["Close"] - prev_close
                        change_percent = (
                            (change_amount / prev_close) * 100 if prev_close != 0 else 0
                        )
                    else:
                        change_amount = 0
                        change_percent = 0

                    # Create record
                    record = {
                        "index": name,
                        "date": date_str,
                        "open": round(float(row["Open"]), 2) if pd.notna(row["Open"]) else None,
                        "high": round(float(row["High"]), 2) if pd.notna(row["High"]) else None,
                        "low": round(float(row["Low"]), 2) if pd.notna(row["Low"]) else None,
                        "close": round(float(row["Close"]), 2),
                        "change_amount": round(float(change_amount), 2),
                        "change_percent": round(float(change_percent), 2),
                        "volume": int(row["Volume"]) if pd.notna(row["Volume"]) else None,
                    }

                    # Save to S3
                    if self.use_s3 and self._save_to_s3(name, record, date_obj):
                        s3_uploaded_count += 1

                    # Track local changes
                    if date_str in existing_data:
                        updated_count += 1
                    else:
                        new_count += 1

                    existing_data[date_str] = record

                # Save local data
                self._save_local_data(name, existing_data)

                print(f"   ✅ {name}:")
                print(f"      로컬: {new_count}개 신규, {updated_count}개 업데이트")
                if self.use_s3:
                    print(f"      S3: {s3_uploaded_count}개 업로드")

                results[name] = {
                    "new": new_count,
                    "updated": updated_count,
                    "s3_uploaded": s3_uploaded_count if self.use_s3 else 0,
                }

            except Exception as e:
                print(f"   ❌ {name} 실패: {e}")
                results[name] = {"error": str(e)}

        print(f"\n✨ 완료! 데이터 저장 위치:")
        print(f"   로컬: {self.data_dir}/")
        if self.use_s3:
            print(f"   S3: s3://{S3_BUCKET_NAME}/stock-indices/")
        return results

    def _has_valid_ohlc(self, record: Dict) -> bool:
        """Check if a record has valid (non-zero) OHLC data."""
        return (
            record.get("open", 0) > 0
            and record.get("high", 0) > 0
            and record.get("low", 0) > 0
            and record.get("close", 0) > 0
        )

    def get_latest(self) -> Dict:
        """
        Get the latest data for both indices from local storage.
        Skips entries with invalid (zero) OHLC values.

        UAT mode: Returns data on or before UAT_DATE
        Normal mode: Returns the most recent data with valid OHLC
        """
        result = {}
        uat_date = get_uat_date()

        for index in self.indices.keys():
            data = self._load_local_data(index)
            if data:
                if uat_date:
                    valid_dates = [d for d in data.keys() if d <= uat_date]
                else:
                    valid_dates = list(data.keys())

                # Sort by date descending and find first entry with valid OHLC
                for date in sorted(valid_dates, reverse=True):
                    if self._has_valid_ohlc(data[date]):
                        result[index] = data[date]
                        break

        return result

    def get_history(self, index_type: str, days: int = 30) -> List[Dict]:
        """
        Get historical data for KOSPI or KOSDAQ from local storage.

        Args:
            index_type: 'KOSPI' or 'KOSDAQ'
            days: Number of days (default: 30)

        UAT mode: Returns data up to UAT_DATE
        Normal mode: Returns the most recent N days
        """
        if index_type not in self.indices:
            raise ValueError(f"Invalid index. Choose 'KOSPI' or 'KOSDAQ'")

        data = self._load_local_data(index_type)

        if not data:
            return []

        uat_date = get_uat_date()

        if uat_date:
            # UAT 모드: UAT 날짜 이전의 데이터만 필터링
            valid_dates = [d for d in data.keys() if d <= uat_date]
            sorted_dates = sorted(valid_dates, reverse=True)[:days]
        else:
            # 일반 모드: 최신 N일
            sorted_dates = sorted(data.keys(), reverse=True)[:days]

        # oldest to newest
        sorted_dates.reverse()

        return [data[date] for date in sorted_dates]

    def get_summary(self) -> Dict:
        """
        Get summary statistics for KOSPI and KOSDAQ from local storage.

        UAT mode: Calculates statistics based on data up to UAT_DATE
        Normal mode: Uses all available data
        """
        summary = {}
        uat_date = get_uat_date()

        for index in self.indices.keys():
            data = self._load_local_data(index)

            if data:
                if uat_date:
                    # UAT 모드: UAT 날짜 이전 데이터만
                    valid_data = {d: v for d, v in data.items() if d <= uat_date}
                else:
                    valid_data = data

                if not valid_data:
                    continue

                # Last 30 days
                sorted_dates = sorted(valid_data.keys(), reverse=True)[:30]
                prices = [valid_data[d]["close"] for d in sorted_dates]

                latest = valid_data[sorted_dates[0]]

                # Calculate 52-week high/low if we have enough data
                sorted_all = sorted(valid_data.keys(), reverse=True)[
                    :252
                ]  # ~1 year of trading days
                year_prices = (
                    [valid_data[d]["close"] for d in sorted_all] if len(sorted_all) > 0 else prices
                )

                summary[index] = {
                    "latest_price": latest["close"],
                    "latest_change": latest["change_percent"],
                    "latest_date": latest["date"],
                    "latest_volume": latest.get("volume"),
                    "30d_high": max(prices) if prices else None,
                    "30d_low": min(prices) if prices else None,
                    "30d_avg": round(sum(prices) / len(prices), 2) if prices else None,
                    "52w_high": max(year_prices) if year_prices else None,
                    "52w_low": min(year_prices) if year_prices else None,
                    "data_points": len(valid_data),
                }

        return summary


# Quick functions for command-line use


def setup_initial_data():
    """Initial setup - fetch 365 days of data."""
    manager = StockindexManager()
    return manager.fetch_historical(days=365)


def daily_update():
    """Daily update - run at 3:35 PM KST."""
    manager = StockindexManager()
    return manager.fetch_daily()


def view_latest():
    """View latest prices."""
    manager = StockindexManager()
    latest = manager.get_latest()

    print("\n" + "=" * 50)
    print(f"한국 주식시장 현재가")
    print("=" * 50)

    for index, data in latest.items():
        emoji = "📈" if data["change_percent"] >= 0 else "📉"
        print(f"\n{emoji} {index}")
        print(f"   종가: {data['close']:,}원")
        print(f"   변동: {data['change_amount']:+,.0f}원 ({data['change_percent']:+.2f}%)")
        print(f"   거래량: {data['volume']:,}" if data["volume"] else "   거래량: N/A")
        print(f"   날짜: {data['date']}")

    return latest


def view_summary():
    """View market summary statistics."""
    manager = StockindexManager()
    summary = manager.get_summary()

    print("\n" + "=" * 50)
    print(f"한국 주식시장 통계")
    print("=" * 50)

    for index, stats in summary.items():
        emoji = "📈" if stats["latest_change"] >= 0 else "📉"
        print(f"\n{emoji} {index}")
        print(f"   현재가: {stats['latest_price']:,}원 ({stats['latest_change']:+.2f}%)")
        print(f"   30일 고/저: {stats['30d_high']:,}원 / {stats['30d_low']:,}원")
        print(f"   30일 평균: {stats['30d_avg']:,}원")
        print(f"   52주 고/저: {stats['52w_high']:,}원 / {stats['52w_low']:,}원")
        print(f"   데이터 포인트: {stats['data_points']}개")

    return summary


if __name__ == "__main__":
    import sys

    if len(sys.argv) > 1:
        if sys.argv[1] == "setup":
            setup_initial_data()
        elif sys.argv[1] == "update":
            daily_update()
        elif sys.argv[1] == "view":
            view_latest()
        elif sys.argv[1] == "summary":
            view_summary()
    else:
        view_latest()
