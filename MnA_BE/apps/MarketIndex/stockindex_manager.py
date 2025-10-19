"""
Korean stock market data manager (KOSPI & KOSDAQ only).
Fetches data at 3:35 PM KST after market close.
Stores data in S3 with partitioning.
"""

import json
import os
from datetime import datetime, timedelta
from typing import Dict, List, Optional
import yfinance as yf
import pandas as pd
from pathlib import Path
import boto3
from botocore.exceptions import ClientError

# S3 설정
S3_BUCKET_NAME = "swpp-12-bucket"
S3_REGION = "ap-northeast-2"

# Get the directory where this script is located
BASE_DIR = Path(__file__).resolve().parent

class StockindexManager:
    """
    Manages KOSPI and KOSDAQ data in S3.
    Designed to run at 3:35 PM KST after market close.
    """
    
    def __init__(self, data_dir_name: str = "stockindex"):
        """
        Initialize the manager for Korean markets.
        
        Args:
            data_dir_name: Local directory for temporary files (default: "stockindex")
        """
        self.data_dir = BASE_DIR / data_dir_name
        self.data_dir.mkdir(exist_ok=True)
        
        # S3 클라이언트 초기화
        self.s3_client = boto3.client('s3', region_name=S3_REGION)
        
        # Korean indices only
        self.indices = {
            'KOSPI': '^KS11',
            'KOSDAQ': '^KQ11'
        }
        
        self.max_days = 365  # Keep 1 year of history
        self.market_close_time = "15:30"  # Korean market closes at 3:30 PM
    
    def _get_local_file_path(self, index_type: str) -> Path:
        """Get the local JSON file path for an index."""
        return self.data_dir / f"{index_type}.json"
    
    def _get_s3_key(self, index_type: str, date_obj: datetime) -> str:
        """Get S3 key with partitioning."""
        year = date_obj.strftime("%Y")
        month = date_obj.strftime("%-m")  # 0 없이
        day = date_obj.strftime("%-d")    # 0 없이
        return f"stock-indices/year={year}/month={month}/day={day}/{index_type}.json"
    
    def _load_from_s3(self, index_type: str, date_obj: datetime) -> Optional[Dict]:
        """Load data from S3."""
        try:
            s3_key = self._get_s3_key(index_type, date_obj)
            response = self.s3_client.get_object(Bucket=S3_BUCKET_NAME, Key=s3_key)
            data = json.loads(response['Body'].read().decode('utf-8'))
            return data
        except ClientError as e:
            if e.response['Error']['Code'] == 'NoSuchKey':
                return None
            print(f"[S3] Error loading {index_type}: {e}")
            return None
        except Exception as e:
            print(f"[S3] Error: {e}")
            return None
    
    def _save_to_s3(self, index_type: str, data: Dict, date_obj: datetime):
        """Save data to S3."""
        try:
            s3_key = self._get_s3_key(index_type, date_obj)
            
            # Convert to JSON string
            json_data = json.dumps(data, ensure_ascii=False, indent=2)
            
            # Upload to S3
            self.s3_client.put_object(
                Bucket=S3_BUCKET_NAME,
                Key=s3_key,
                Body=json_data.encode('utf-8'),
                ContentType='application/json'
            )
            
            print(f"[S3] ✓ Uploaded: s3://{S3_BUCKET_NAME}/{s3_key}")
            return True
        except Exception as e:
            print(f"[S3] ✗ Upload failed for {index_type}: {e}")
            return False
    
    def _load_data(self, index_type: str) -> Dict:
        """Load data from local JSON file (for backward compatibility)."""
        file_path = self._get_local_file_path(index_type)
        
        if file_path.exists():
            with open(file_path, 'r', encoding='utf-8') as f:
                return json.load(f)
        return {}
    
    def _save_data(self, index_type: str, data: Dict):
        """Save data to local JSON file."""
        file_path = self._get_local_file_path(index_type)
        
        # Sort by date and keep only recent data
        sorted_dates = sorted(data.keys(), reverse=True)[:self.max_days]
        cleaned_data = {date: data[date] for date in sorted_dates}
        
        with open(file_path, 'w', encoding='utf-8') as f:
            json.dump(cleaned_data, f, ensure_ascii=False, indent=2, sort_keys=True)
    
    def fetch_daily(self):
        """
        Fetch today's closing prices for KOSPI and KOSDAQ.
        Save to S3 with date partitioning.
        Run this at 3:35 PM KST (after market close).
        """
        current_time = datetime.now()
        print(f"\n{'='*50}")
        print(f"한국 주식시장 종가 업데이트")
        print(f"시간: {current_time.strftime('%Y-%m-%d %H:%M:%S')}")
        print('='*50)
        
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
                date_str = latest_date.strftime('%Y-%m-%d')
                date_obj = latest_date.to_pydatetime()
                
                # Calculate change from previous day
                if len(hist) >= 2:
                    prev_close = float(hist['Close'].iloc[-2])
                else:
                    prev_close = float(latest_row['Close'])
                
                change_amount = float(latest_row['Close']) - prev_close
                change_percent = (change_amount / prev_close) * 100 if prev_close != 0 else 0
                
                # Create record
                record = {
                    'index': name,
                    'date': date_str,
                    'open': round(float(latest_row['Open']), 2) if pd.notna(latest_row['Open']) else None,
                    'high': round(float(latest_row['High']), 2) if pd.notna(latest_row['High']) else None,
                    'low': round(float(latest_row['Low']), 2) if pd.notna(latest_row['Low']) else None,
                    'close': round(float(latest_row['Close']), 2),
                    'change_amount': round(change_amount, 2),
                    'change_percent': round(change_percent, 2),
                    'volume': int(latest_row['Volume']) if pd.notna(latest_row['Volume']) else None,
                    'fetched_at': current_time.isoformat()
                }
                
                # Save to S3
                self._save_to_s3(name, record, date_obj)
                
                # Also save locally (optional)
                local_data = self._load_data(name)
                local_data[date_str] = record
                self._save_data(name, local_data)
                
                # Display with emoji
                emoji = "📈" if change_percent >= 0 else "📉"
                print(f"{emoji} {name}: {record['close']:,}원 ({change_percent:+.2f}%)")
                results[name] = record
                
            except Exception as e:
                print(f"❌ {name} 실패: {e}")
                results[name] = {'error': str(e)}
        
        print(f"\n✅ 업데이트 완료!")
        return results
    
    def fetch_historical(self, days: int = 365):
        """
        Fetch historical data for Korean markets and save to S3.
        Run this ONCE to initialize data.
        
        Args:
            days: Number of days to fetch (default: 365)
        """
        print(f"\n{'='*50}")
        print(f"한국 주식시장 {days}일 데이터 다운로드 및 S3 업로드")
        print(f"Fetching {days} days of Korean market data")
        print('='*50)
        
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
                
                new_count = 0
                updated_count = 0
                cutoff_date = (datetime.now() - timedelta(days=days)).date()
                
                for i, (date, row) in enumerate(hist.iterrows()):
                    # Skip old dates
                    if date.date() < cutoff_date:
                        continue
                    
                    date_str = date.strftime('%Y-%m-%d')
                    date_obj = date.to_pydatetime()
                    
                    # Calculate change
                    if i > 0:
                        prev_close = hist['Close'].iloc[i-1]
                        change_amount = row['Close'] - prev_close
                        change_percent = (change_amount / prev_close) * 100 if prev_close != 0 else 0
                    else:
                        change_amount = 0
                        change_percent = 0
                    
                    # Create record
                    record = {
                        'index': name,
                        'date': date_str,
                        'open': round(float(row['Open']), 2) if pd.notna(row['Open']) else None,
                        'high': round(float(row['High']), 2) if pd.notna(row['High']) else None,
                        'low': round(float(row['Low']), 2) if pd.notna(row['Low']) else None,
                        'close': round(float(row['Close']), 2),
                        'change_amount': round(float(change_amount), 2),
                        'change_percent': round(float(change_percent), 2),
                        'volume': int(row['Volume']) if pd.notna(row['Volume']) else None
                    }
                    
                    # Save to S3
                    if self._save_to_s3(name, record, date_obj):
                        new_count += 1
                
                print(f"   ✅ {name}: {new_count}개 S3 업로드 완료")
                results[name] = {'uploaded': new_count}
                
            except Exception as e:
                print(f"   ❌ {name} 실패: {e}")
                results[name] = {'error': str(e)}
        
        print(f"\n✨ 완료! 데이터가 S3에 저장되었습니다.")
        return results


# Quick functions for command-line use

def setup_initial_data():
    """Initial setup - fetch 365 days of data and upload to S3."""
    manager = StockindexManager()
    return manager.fetch_historical(days=365)

def daily_update():
    """Daily update - run at 3:35 PM KST."""
    manager = StockindexManager()
    return manager.fetch_daily()


if __name__ == "__main__":
    import sys
    
    if len(sys.argv) > 1:
        if sys.argv[1] == "setup":
            setup_initial_data()
        elif sys.argv[1] == "update":
            daily_update()
    else:
        daily_update()