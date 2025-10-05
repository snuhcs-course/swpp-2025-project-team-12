# Iteration 1 demo of Daily insight

## 1. implemented features

<img width="1920" height="1080" alt="architecture_iter1" src="https://github.com/user-attachments/assets/e42841f3-d0c7-408b-9355-802e09037361" />

- Index crawler
- Articles crawler
- Stock information crawler (Brief information about corporates and their financial data)

## 2. how to set environment

### 2.1. prepare repository
First, clone git repository
```
git clone [url]
```

And then, move to the project directory
```
cd swpp-2025-project-team-12
```

Then switch to the 'iteration-1-demo' branch
```
git checkout iteration-1-demo
```

### 2.2. prepare virtual environment

Create a new environment with Anaconda3 and activate it
You can set the environment name freely

```
conda create -n myenv
conda activate myenv
```

Move to the demo directory named MnA_BE
```
cd MnA_BE
```

Install the modules needed
```
pip install -r requirements.txt
```

## 3. how to run demo

Make sure you have activated the virtual environment and switched to the branch
And you have to be: .../swpp-2025-project-team-12/MnA_BE

...so it will be like
```
(myenv) ~/swpp-2025-project-team-12/MnA_BE git:(iteration-1-demo)
```

### 3.1. Index crawler

```
python -c "from apps.MarketIndex.stockindex_manager import setup_initial_data; setup_initial_data()"
```

You can check the results at:
.../swpp-2025-project-team-12/MnA_BE/apps/MarketIndex/stockindex/KOSPI.json
.../swpp-2025-project-team-12/MnA_BE/apps/MarketIndex/stockindex/KOSDAQ.json
```
cat apps/MarketIndex/stockindex/KOSPI.json
cat apps/MarketIndex/stockindex/KOSDAQ.json
```

### 3.2. Articles crawler

```
python apps/articles/crawler_main.py
```

You can check the results at:
.../swpp-2025-project-team-12/MnA_BE/apps/articles/[date]/business_top50.json
[date] is the date today (e.g. 20251005)

### 3.3. Stock information crawler

```
python apps/Fianance/finance_crawler.py
```

You can check the result just at the terminal (will be printed out)
