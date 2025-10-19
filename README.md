# Iteration 1 demo of Daily insight

## 1. implemented features

<img width="415" height="446" alt="architecture_iter1" src="https://github.com/user-attachments/assets/0957eca9-d3ff-492c-9751-c91cf1f211bd" />

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

Make sure you have activated the virtual environment and switched to the branch <br>
And you have to be: .../swpp-2025-project-team-12/MnA_BE <br>

...so it will be like
```
(myenv) ~/swpp-2025-project-team-12/MnA_BE git:(iteration-1-demo)
```

### 3.1. Index crawler

```
python -c "from apps.MarketIndex.stockindex_manager import setup_initial_data; setup_initial_data()"
```

You can check the results at: <br>
.../swpp-2025-project-team-12/MnA_BE/apps/MarketIndex/stockindex/KOSPI.json <br>
.../swpp-2025-project-team-12/MnA_BE/apps/MarketIndex/stockindex/KOSDAQ.json <br>
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
python apps/Finance/finance_crawler.py
```

You can check the result just at the terminal (will be printed out)

## 4. demo video

You can check the demo video for each feature here

### 4.1. Index crawler
[swpp25 team12 iteration 1 demo - 1. Index crawler](https://youtu.be/ipA-jFqFZws)

### 4.2. Articles crawler
[swpp25 team12 iteration 1 demo - 2. Articles crawler](https://youtu.be/YQWbwLg6EsM)

### 4.3. Stock information crawler
[swpp25 team12 iteration 1 demo - 3. Stock information crawler](https://youtu.be/7EzlZLypGA0)
