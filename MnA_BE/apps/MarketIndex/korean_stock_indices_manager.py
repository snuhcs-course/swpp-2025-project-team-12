"""
Korean stock market data manager (KOSPI & KOSDAQ only).
Fetches data at 3:35 PM KST after market close.
Stores data locally in JSON files.
"""

import json
import os
from datetime import datetime, timedelta
from typing import Dict, List, Optional
import yfinance as yf
import pandas as pd
from pathlib import Path

# Get the directory where this script (korean_stock_indices_manager.py) is located.
# This ensures that the path is correct no matter where the script is run from.
BASE_DIR = Path(__file__).resolve().parent

class KoreanStockIndicesManager:
    """
    Manages KOSPI and KOSDAQ data in local JSON files.
    Designed to run at 3:35 PM KST after market close.
    """
    
    def __init__(self, data_dir_name: str = "korean_stock_indices"):
        """
        Initialize the manager for Korean markets.
        
        Args:
            data_dir_name: Directory to store JSON files (default: "korean_stock_indices")
        """
        # --- START: MODIFY THIS LINE ---
        # Construct a full, absolute path to the data directory.
        self.data_dir = BASE_DIR / data_dir_name
        # --- END: MODIFY THIS LINE ---
        
        self.data_dir.mkdir(exist_ok=True)
        
        # Korean indices only
        self.indices = {
            'KOSPI': '^KS11',
            'KOSDAQ': '^KQ11'
        }
        
        self.max_days = 365  # Keep 1 year of history
        self.market_close_time = "15:30"  # Korean market closes at 3:30 PM
    
    def _get_file_path(self, index_type: str) -> Path:
        """Get the JSON file path for an index."""
        return self.data_dir / f"{index_type}.json"
    
    def _load_data(self, index_type: str) -> Dict:
        """Load data from JSON file."""
        file_path = self._get_file_path(index_type)
        
        if file_path.exists():
            with open(file_path, 'r', encoding='utf-8') as f:
                return json.load(f)
        return {}
    
    def _save_data(self, index_type: str, data: Dict):
        """Save data to JSON file, keeping only last 365 days."""
        file_path = self._get_file_path(index_type)
        
        # Sort by date (newest first) and keep only 365 days
        sorted_dates = sorted(data.keys(), reverse=True)[:self.max_days]
        cleaned_data = {date: data[date] for date in sorted_dates}
        
        # Save with nice formatting
        with open(file_path, 'w', encoding='utf-8') as f:
            json.dump(cleaned_data, f, ensure_ascii=False, indent=2, sort_keys=True)
    
    def fetch_historical(self, days: int = 365):
        """
        Fetch historical data for Korean markets.
        Run this ONCE to initialize data.
        
        Args:
            days: Number of days to fetch (default: 365)
        """
        print(f"\n{'='*50}")
        print(f"한국 주식시장 {days}일 데이터 다운로드")
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
                
                # Load existing data
                existing_data = self._load_data(name)
                
                # Process data
                new_count = 0
                updated_count = 0
                cutoff_date = (datetime.now() - timedelta(days=days)).date()
                
                for i, (date, row) in enumerate(hist.iterrows()):
                    # Skip old dates
                    if date.date() < cutoff_date:
                        continue
                    
                    date_str = date.strftime('%Y-%m-%d')
                    
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
                        'date': date_str,
                        'open': round(float(row['Open']), 2) if pd.notna(row['Open']) else None,
                        'high': round(float(row['High']), 2) if pd.notna(row['High']) else None,
                        'low': round(float(row['Low']), 2) if pd.notna(row['Low']) else None,
                        'close': round(float(row['Close']), 2),
                        'change_amount': round(float(change_amount), 2),
                        'change_percent': round(float(change_percent), 2),
                        'volume': int(row['Volume']) if pd.notna(row['Volume']) else None
                    }
                    
                    if date_str in existing_data:
                        updated_count += 1
                    else:
                        new_count += 1
                    
                    existing_data[date_str] = record
                
                # Save data
                self._save_data(name, existing_data)
                
                print(f"   ✅ {name}: {new_count}개 신규, {updated_count}개 업데이트")
                results[name] = {'new': new_count, 'updated': updated_count}
                
            except Exception as e:
                print(f"   ❌ {name} 실패: {e}")
                results[name] = {'error': str(e)}
        
        print(f"\n✨ 완료! 데이터 저장 위치: {self.data_dir}/")
        return results
    
    def fetch_daily(self):
        """
        Fetch today's closing prices for KOSPI and KOSDAQ.
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
                
                # Load existing data
                existing_data = self._load_data(name)
                
                # Check if already updated today
                if date_str in existing_data:
                    existing_close = existing_data[date_str]['close']
                    if abs(existing_close - float(latest_row['Close'])) < 0.01:
                        print(f"⏭️  {name}: 이미 최신 데이터")
                        continue
                
                # Calculate change from previous day
                if len(hist) >= 2:
                    prev_close = float(hist['Close'].iloc[-2])
                else:
                    # Get from existing data
                    sorted_dates = sorted(existing_data.keys(), reverse=True)
                    if sorted_dates and sorted_dates[0] != date_str:
                        prev_close = existing_data[sorted_dates[0]]['close']
                    else:
                        prev_close = float(latest_row['Close'])
                
                change_amount = float(latest_row['Close']) - prev_close
                change_percent = (change_amount / prev_close) * 100 if prev_close != 0 else 0
                
                # Create record
                record = {
                    'date': date_str,
                    'open': round(float(latest_row['Open']), 2) if pd.notna(latest_row['Open']) else None,
                    'high': round(float(latest_row['High']), 2) if pd.notna(latest_row['High']) else None,
                    'low': round(float(latest_row['Low']), 2) if pd.notna(latest_row['Low']) else None,
                    'close': round(float(latest_row['Close']), 2),
                    'change_amount': round(change_amount, 2),
                    'change_percent': round(change_percent, 2),
                    'volume': int(latest_row['Volume']) if pd.notna(latest_row['Volume']) else None
                }
                
                # Save
                existing_data[date_str] = record
                self._save_data(name, existing_data)
                
                # Display with emoji
                emoji = "📈" if change_percent >= 0 else "📉"
                print(f"{emoji} {name}: {record['close']:,}원 ({change_percent:+.2f}%)")
                results[name] = record
                
            except Exception as e:
                print(f"❌ {name} 실패: {e}")
                results[name] = {'error': str(e)}
        
        print(f"\n✅ 업데이트 완료!")
        return results
    
    def get_latest(self) -> Dict:
        """Get the latest data for both indices."""
        result = {}
        
        for index in self.indices.keys():
            data = self._load_data(index)
            if data:
                latest_date = max(data.keys())
                result[index] = data[latest_date]
        
        return result
    
    def get_history(self, index_type: str, days: int = 30) -> List[Dict]:
        """
        Get historical data for KOSPI or KOSDAQ.
        
        Args:
            index_type: 'KOSPI' or 'KOSDAQ'
            days: Number of days (default: 30)
        """
        if index_type not in self.indices:
            raise ValueError(f"Invalid index. Choose 'KOSPI' or 'KOSDAQ'")
        
        data = self._load_data(index_type)
        
        if not data:
            return []
        
        # Get last N days (oldest to newest)
        sorted_dates = sorted(data.keys(), reverse=True)[:days]
        sorted_dates.reverse()
        
        return [data[date] for date in sorted_dates]
    
    def get_summary(self) -> Dict:
        """Get summary statistics for KOSPI and KOSDAQ."""
        summary = {}
        
        for index in self.indices.keys():
            data = self._load_data(index)
            
            if data:
                # Last 30 days
                sorted_dates = sorted(data.keys(), reverse=True)[:30]
                prices = [data[d]['close'] for d in sorted_dates]
                
                latest = data[sorted_dates[0]]
                
                # Calculate 52-week high/low if we have enough data
                sorted_all = sorted(data.keys(), reverse=True)[:252]  # ~1 year of trading days
                year_prices = [data[d]['close'] for d in sorted_all] if len(sorted_all) > 0 else prices
                
                summary[index] = {
                    'latest_price': latest['close'],
                    'latest_change': latest['change_percent'],
                    'latest_date': latest['date'],
                    'latest_volume': latest.get('volume'),
                    '30d_high': max(prices) if prices else None,
                    '30d_low': min(prices) if prices else None,
                    '30d_avg': round(sum(prices) / len(prices), 2) if prices else None,
                    '52w_high': max(year_prices) if year_prices else None,
                    '52w_low': min(year_prices) if year_prices else None,
                    'data_points': len(data)
                }
        
        return summary


# Quick functions for command-line use

def setup_initial_data():
    """Initial setup - fetch 365 days of data."""
    manager = KoreanStockIndicesManager()
    return manager.fetch_historical(days=365)

def daily_update():
    """Daily update - run at 3:35 PM KST."""
    manager = KoreanStockIndicesManager()
    return manager.fetch_daily()

def view_latest():
    """View latest prices."""
    manager = KoreanStockIndicesManager()
    latest = manager.get_latest()
    
    print("\n" + "="*50)
    print(f"한국 주식시장 현재가")
    print("="*50)
    
    for index, data in latest.items():
        emoji = "📈" if data['change_percent'] >= 0 else "📉"
        print(f"\n{emoji} {index}")
        print(f"   종가: {data['close']:,}원")
        print(f"   변동: {data['change_amount']:+,.0f}원 ({data['change_percent']:+.2f}%)")
        print(f"   거래량: {data['volume']:,}" if data['volume'] else "   거래량: N/A")
        print(f"   날짜: {data['date']}")
    
    return latest


if __name__ == "__main__":
    # Example usage
    import sys
    
    if len(sys.argv) > 1:
        if sys.argv[1] == "setup":
            setup_initial_data()
        elif sys.argv[1] == "update":
            daily_update()
        elif sys.argv[1] == "view":
            view_latest()
    else:
        view_latest()