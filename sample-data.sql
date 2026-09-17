-- Sample data loader for mission-model-hardened.sql
-- Populates clients, accounts, client_accounts, instruments, model_portfolios (dimension tables) plus
-- account_holdings, account_trades, model_portfolio_holdings, account_subscriptions
-- with 10,000 rows each. Run mission-model-hardened.sql first to create the schema.

TRUNCATE TABLE
    account_subscriptions,
    model_portfolio_holdings,
    model_portfolios,
    account_holdings,
    account_trades,
    client_accounts,
    accounts,
    instruments,
    clients
RESTART IDENTITY CASCADE;

-- clients (1,000 rows) with realistic names and birth dates
INSERT INTO clients (name, birth_date)
WITH first_names AS (
  SELECT * FROM (VALUES
    ('James'), ('Mary'), ('Robert'), ('Patricia'), ('Michael'), ('Jennifer'), ('William'), ('Linda'),
    ('David'), ('Barbara'), ('Richard'), ('Susan'), ('Joseph'), ('Jessica'), ('Thomas'), ('Sarah'),
    ('Charles'), ('Karen'), ('Christopher'), ('Nancy'), ('Daniel'), ('Lisa'), ('Matthew'), ('Betty'),
    ('Anthony'), ('Margaret'), ('Donald'), ('Sandra'), ('Mark'), ('Ashley'), ('Steven'), ('Donna'),
    ('Andrew'), ('Carol'), ('Kenneth'), ('Melissa'), ('Joshua'), ('Deborah'), ('Kevin'), ('Stephanie'),
    ('Brian'), ('Rebecca'), ('George'), ('Sharon'), ('Edward'), ('Kathleen'), ('Ronald'), ('Shirley'),
    ('Timothy'), ('Cynthia'), ('Jason'), ('Angela'), ('Jeffrey'), ('Helen'), ('Ryan'), ('Anna')
  ) AS t(name)
),
last_names AS (
  SELECT * FROM (VALUES
    ('Smith'), ('Johnson'), ('Williams'), ('Brown'), ('Jones'), ('Garcia'), ('Miller'), ('Davis'),
    ('Rodriguez'), ('Martinez'), ('Hernandez'), ('Lopez'), ('Gonzalez'), ('Wilson'), ('Anderson'), ('Thomas'),
    ('Taylor'), ('Moore'), ('Jackson'), ('Martin'), ('Perez'), ('Ibanez'), ('Ramirez'), ('Flores'),
    ('White'), ('Harris'), ('Martin'), ('Thompson'), ('Lee'), ('Clark'), ('Lewis'), ('Walker'),
    ('Hall'), ('Young'), ('Sanchez'), ('Morris'), ('Rogers'), ('Morgan'), ('Peterson'), ('Cooper'),
    ('Reed'), ('Bell'), ('Gomez'), ('Murray'), ('Freeman'), ('Wells'), ('Webb'), ('Simpson'),
    ('Stevens'), ('Tucker'), ('Porter'), ('Hunter'), ('Hicks'), ('Crawford'), ('Henry'), ('Boyd')
  ) AS t(name)
)
SELECT fn.name || ' ' || ln.name, DATE '1950-01-01' + (random() * 25000)::INT
FROM first_names fn
CROSS JOIN last_names ln
LIMIT 1000;

-- accounts (5,000 rows; 5 account types, roughly 1000 of each)
INSERT INTO accounts (account_type)
SELECT CASE (i % 5)
  WHEN 0 THEN 'cash'
  WHEN 1 THEN 'margin'
  WHEN 2 THEN 'retirement'
  WHEN 3 THEN 'investment'
  ELSE 'savings'
END
FROM generate_series(1, 5000) AS i;

-- client_accounts junction table (1-3 accounts per client, ~2500 rows)
INSERT INTO client_accounts (client_id, account_id)
SELECT
  ((i / 3) % 1000) + 1,
  (i % 5000) + 1
FROM generate_series(0, 2499) AS i;

-- instruments (334 rows) with real ticker symbols and asset classes
INSERT INTO instruments (name, ticker, asset_class)
WITH real_instruments AS (
  SELECT * FROM (VALUES
    -- Stocks (100)
    ('Apple Inc', 'AAPL', 'stock'), ('Microsoft Corporation', 'MSFT', 'stock'), ('Amazon.com Inc', 'AMZN', 'stock'),
    ('Alphabet Inc', 'GOOGL', 'stock'), ('Tesla Inc', 'TSLA', 'stock'), ('NVIDIA Corporation', 'NVDA', 'stock'),
    ('Meta Platforms Inc', 'META', 'stock'), ('Berkshire Hathaway', 'BRK', 'stock'), ('JPMorgan Chase', 'JPM', 'stock'),
    ('Visa Inc', 'V', 'stock'), ('Mastercard Inc', 'MA', 'stock'), ('Procter & Gamble', 'PG', 'stock'),
    ('Johnson & Johnson', 'JNJ', 'stock'), ('Coca-Cola Company', 'KO', 'stock'), ('Walmart Inc', 'WMT', 'stock'),
    ('Boeing Company', 'BA', 'stock'), ('Intel Corporation', 'INTC', 'stock'), ('AMD Inc', 'AMD', 'stock'),
    ('Netflix Inc', 'NFLX', 'stock'), ('Adobe Inc', 'ADBE', 'stock'), ('Cisco Systems', 'CSCO', 'stock'),
    ('Oracle Corporation', 'ORCL', 'stock'), ('Salesforce Inc', 'CRM', 'stock'), ('IBM Corporation', 'IBM', 'stock'),
    ('Chevron Corporation', 'CVX', 'stock'), ('ExxonMobil Corporation', 'XOM', 'stock'), ('ConocoPhillips', 'COP', 'stock'),
    ('Schlumberger Ltd', 'SLB', 'stock'), ('Valero Energy', 'VLO', 'stock'), ('Antero Midstream', 'AM', 'stock'),
    ('Home Depot Inc', 'HD', 'stock'), ('Lowes Companies', 'LOW', 'stock'), ('Target Corporation', 'TGT', 'stock'),
    ('Best Buy Co', 'BBY', 'stock'), ('Gap Inc', 'GPS', 'stock'), ('Nike Inc', 'NKE', 'stock'),
    ('Adidas AG', 'ADSY', 'stock'), ('Starbucks Corporation', 'SBUX', 'stock'), ('McDonalds Corp', 'MCD', 'stock'),
    ('Yum Brands', 'YUM', 'stock'), ('Dominos Pizza', 'DPZ', 'stock'), ('Chipotle Mexican Grill', 'CMG', 'stock'),
    ('Uber Technologies', 'UBER', 'stock'), ('Lyft Inc', 'LYFT', 'stock'), ('AirBnB Inc', 'ABNB', 'stock'),
    ('PayPal Holdings', 'PYPL', 'stock'), ('Square Inc', 'SQ', 'stock'), ('Block Inc', 'BLOCK', 'stock'),
    ('Stripe Holdings', 'STRIPE', 'stock'), ('Shopify Inc', 'SHOP', 'stock'), ('Docusign Inc', 'DOCU', 'stock'),
    ('Zoom Video Communications', 'ZM', 'stock'), ('ServiceNow Inc', 'NOW', 'stock'), ('Workday Inc', 'WDAY', 'stock'),
    ('Synopsys Inc', 'SNPS', 'stock'), ('Cadence Design', 'CDNS', 'stock'), ('Asml Holdings', 'ASML', 'stock'),
    ('Broadcom Inc', 'AVGO', 'stock'), ('Qualcomm Inc', 'QCOM', 'stock'), ('Applied Materials', 'AMAT', 'stock'),
    ('Lam Research', 'LRCX', 'stock'), ('ASAP Semiconductor', 'ASAP', 'stock'), ('Analog Devices', 'ADI', 'stock'),
    ('Monolithic Power Systems', 'MPWR', 'stock'), ('Advanced Energy Industries', 'AEIS', 'stock'), ('Marvell Technology', 'MRVL', 'stock'),
    ('Micron Technology', 'MU', 'stock'), ('Western Digital', 'WDC', 'stock'), ('Corsair Gaming', 'CRSR', 'stock'),
    ('Turtle Beach', 'HEAR', 'stock'), ('SoundThetics Inc', 'SNTS', 'stock'), ('Dolby Laboratories', 'DLB', 'stock'),
    ('CVS Health Corporation', 'CVS', 'stock'), ('Walgreens Boots Alliance', 'WBA', 'stock'), ('Pfizer Inc', 'PFE', 'stock'),
    ('Moderna Inc', 'MRNA', 'stock'), ('AstraZeneca PLC', 'AZN', 'stock'), ('Merck Co', 'MRK', 'stock'),
    ('Eli Lilly and Company', 'LLY', 'stock'), ('Thermo Fisher Scientific', 'TMO', 'stock'), ('Illumina Inc', 'ILMN', 'stock'),
    ('Qiagen NV', 'QGEN', 'stock'), ('BioRad Laboratories', 'BIO', 'stock'), ('Avantor Inc', 'AVTR', 'stock'),
    ('Charles River Labs', 'CRL', 'stock'), ('Zoetis Inc', 'ZTS', 'stock'), ('Neogen Corporation', 'NEOG', 'stock'),
    -- Bonds (100)
    ('US Treasury 2Y', 'UST2', 'bond'), ('US Treasury 5Y', 'UST5', 'bond'), ('US Treasury 10Y', 'UST10', 'bond'),
    ('US Treasury 20Y', 'UST20', 'bond'), ('US Treasury 30Y', 'UST30', 'bond'), ('UK Gilts 10Y', 'GBL10', 'bond'),
    ('German Bunds 10Y', 'BUN10', 'bond'), ('Japanese JGB 10Y', 'JGB10', 'bond'), ('Canadian Bond 10Y', 'CAD10', 'bond'),
    ('Australian Bond 10Y', 'AUD10', 'bond'), ('Swiss Bond 10Y', 'CHB10', 'bond'), ('Sweden Bond 10Y', 'SEB10', 'bond'),
    ('Norway Bond 10Y', 'NOB10', 'bond'), ('New Zealand Bond 10Y', 'NZB10', 'bond'), ('Singapore Bond 10Y', 'SGB10', 'bond'),
    ('Hong Kong Bond 10Y', 'HKB10', 'bond'), ('India Bond 10Y', 'INB10', 'bond'), ('Brazil Bond 10Y', 'BRB10', 'bond'),
    ('Mexico Bond 10Y', 'MXB10', 'bond'), ('Russia Bond 10Y', 'RUB10', 'bond'), ('South Africa Bond 10Y', 'ZAB10', 'bond'),
    ('Egypt Bond 10Y', 'EGB10', 'bond'), ('Nigeria Bond 10Y', 'NGB10', 'bond'), ('UAE Bond 10Y', 'AED10', 'bond'),
    ('Saudi Bond 10Y', 'SAB10', 'bond'), ('Israel Bond 10Y', 'ILB10', 'bond'), ('Turkey Bond 10Y', 'TRB10', 'bond'),
    ('Greece Bond 10Y', 'GRB10', 'bond'), ('Portugal Bond 10Y', 'PTB10', 'bond'), ('Spain Bond 10Y', 'ESB10', 'bond'),
    ('Italy Bond 10Y', 'ITB10', 'bond'), ('Ireland Bond 10Y', 'IEB10', 'bond'), ('Belgium Bond 10Y', 'BEB10', 'bond'),
    ('France Bond 10Y', 'FRB10', 'bond'), ('Netherlands Bond 10Y', 'NLB10', 'bond'), ('Austria Bond 10Y', 'ATB10', 'bond'),
    ('Corporate AAA Bond', 'CAAA', 'bond'), ('Corporate AA Bond', 'CAAB', 'bond'), ('Corporate A Bond', 'CACA', 'bond'),
    ('Corporate BBB Bond', 'CBBB', 'bond'), ('Corporate BB Bond', 'CBBC', 'bond'), ('Corporate B Bond', 'CBCD', 'bond'),
    ('High Yield Bond 1', 'HYB1', 'bond'), ('High Yield Bond 2', 'HYB2', 'bond'), ('High Yield Bond 3', 'HYB3', 'bond'),
    ('Municipal Bond 1', 'MUB1', 'bond'), ('Municipal Bond 2', 'MUB2', 'bond'), ('Municipal Bond 3', 'MUB3', 'bond'),
    ('Inflation Bond 1', 'INB1', 'bond'), ('Inflation Bond 2', 'INB2', 'bond'), ('Floating Rate Bond 1', 'FLB1', 'bond'),
    ('Floating Rate Bond 2', 'FLB2', 'bond'), ('Convertible Bond 1', 'CVB1', 'bond'), ('Convertible Bond 2', 'CVB2', 'bond'),
    ('Green Bond 1', 'GRB1', 'bond'), ('Green Bond 2', 'GRB2', 'bond'), ('ESG Bond 1', 'ESB1', 'bond'),
    ('ESG Bond 2', 'ESB2', 'bond'), ('Sustainability Bond 1', 'SUS1', 'bond'), ('Sustainability Bond 2', 'SUS2', 'bond'),
    ('Emerging Market Bond 1', 'EMB1', 'bond'), ('Emerging Market Bond 2', 'EMB2', 'bond'), ('Emerging Market Bond 3', 'EMB3', 'bond'),
    -- Cryptocurrencies (150)
    ('Bitcoin', 'BTC', 'crypto'), ('Ethereum', 'ETH', 'crypto'), ('Tether', 'USDT', 'crypto'), ('USD Coin', 'USDC', 'crypto'),
    ('Binance Coin', 'BNB', 'crypto'), ('Solana', 'SOL', 'crypto'), ('Ripple', 'XRP', 'crypto'), ('Polkadot', 'DOT', 'crypto'),
    ('Dogecoin', 'DOGE', 'crypto'), ('Cardano', 'ADA', 'crypto'), ('Polygon', 'MATIC', 'crypto'), ('Litecoin', 'LTC', 'crypto'),
    ('Bitcoin Cash', 'BCH', 'crypto'), ('Stellar', 'XLM', 'crypto'), ('Chainlink', 'LINK', 'crypto'), ('Cosmos', 'ATOM', 'crypto'),
    ('Monero', 'XMR', 'crypto'), ('Zcash', 'ZEC', 'crypto'), ('Dash', 'DASH', 'crypto'), ('NEO', 'NEO', 'crypto'),
    ('EOS', 'EOS', 'crypto'), ('Tron', 'TRX', 'crypto'), ('Iota', 'MIOTA', 'crypto'), ('Vechain', 'VET', 'crypto'),
    ('Theta', 'THETA', 'crypto'), ('Algorand', 'ALGO', 'crypto'), ('Flow', 'FLOW', 'crypto'), ('Fantom', 'FTM', 'crypto'),
    ('Avalanche', 'AVAX', 'crypto'), ('Harmony', 'ONE', 'crypto'), ('Elrond', 'EGLD', 'crypto'), ('Zilliqa', 'ZIL', 'crypto'),
    ('Tezos', 'XTZ', 'crypto'), ('Celo', 'CELO', 'crypto'), ('Bitcoin SV', 'BSV', 'crypto'), ('Huobi Token', 'HT', 'crypto'),
    ('OKB Token', 'OKB', 'crypto'), ('Uniswap', 'UNI', 'crypto'), ('Aave', 'AAVE', 'crypto'), ('Maker', 'MKR', 'crypto'),
    ('Curve DAO', 'CRV', 'crypto'), ('Lido DAO', 'LDO', 'crypto'), ('Convex', 'CVX', 'crypto'), ('Balancer', 'BAL', 'crypto'),
    ('Yearn Finance', 'YFI', 'crypto'), ('SushiSwap', 'SUSHI', 'crypto'), ('PancakeSwap', 'CAKE', 'crypto'), ('Dydx', 'DYDX', 'crypto'),
    ('Compound', 'COMP', 'crypto'), ('Flux', 'FLUX', 'crypto'), ('Ankr', 'ANKR', 'crypto'), ('Akash Network', 'AKT', 'crypto'),
    ('Internet Computer', 'ICP', 'crypto'), ('Filecoin', 'FIL', 'crypto'), ('Arweave', 'AR', 'crypto'), ('Helium', 'HNT', 'crypto'),
    ('Render Network', 'RNDR', 'crypto'), ('Livepeer', 'LPT', 'crypto'), ('Immutable X', 'IMX', 'crypto'), ('Aptos', 'APT', 'crypto'),
    ('Sui', 'SUI', 'crypto'), ('SEI', 'SEI', 'crypto'), ('Pyth Network', 'PYTH', 'crypto'), ('Wormhole', 'WORM', 'crypto'),
    ('Pendle', 'PENDLE', 'crypto'), ('Arbitrum', 'ARB', 'crypto'), ('Optimism', 'OP', 'crypto'), ('Starknet', 'STRK', 'crypto'),
    ('Linea', 'LINEA', 'crypto'), ('Blast', 'BLAST', 'crypto'), ('Scroll', 'SCRL', 'crypto'), ('Mantle', 'MNT', 'crypto'),
    ('Manta Network', 'MANTA', 'crypto'), ('Ternoa', 'CAPS', 'crypto'), ('Phala Network', 'PHA', 'crypto'), ('Kusama', 'KSM', 'crypto'),
    ('Moonriver', 'MOVR', 'crypto'), ('Hedera', 'HBAR', 'crypto'), ('Nervos', 'CKB', 'crypto'), ('Stacks', 'STX', 'crypto'),
    ('Dfinity', 'DFNT', 'crypto'), ('Mina Protocol', 'MINA', 'crypto'), ('Astar', 'ASTR', 'crypto'), ('OKX Exchange Token', 'OKT', 'crypto'),
    ('Gate Token', 'GT', 'crypto'), ('Kucoin Token', 'KCS', 'crypto'), ('Bybit Token', 'BIT', 'crypto'), ('Kraken Token', 'KRAK', 'crypto'),
    -- ETFs (150)
    ('Vanguard S&P 500 ETF', 'VOO', 'etf'), ('iShares Core S&P 500 ETF', 'IVV', 'etf'), ('SPDR S&P 500 ETF Trust', 'SPY', 'etf'),
    ('Vanguard Total US Stock', 'VTI', 'etf'), ('iShares Core US Total', 'ITOT', 'etf'), ('Schwab US Total Stock', 'SWUS', 'etf'),
    ('Vanguard Dividend Appreciation', 'VIG', 'etf'), ('iShares Select Dividend', 'DVY', 'etf'), ('SPDR S&P Dividend', 'SDY', 'etf'),
    ('Vanguard Growth ETF', 'VUG', 'etf'), ('iShares Russell 1000 Growth', 'IWF', 'etf'), ('SPDR S&P 500 Growth', 'SPLG', 'etf'),
    ('Vanguard Value ETF', 'VTV', 'etf'), ('iShares Russell 1000 Value', 'IWD', 'etf'), ('SPDR S&P 500 Value', 'SPYV', 'etf'),
    ('Vanguard Small Cap ETF', 'VB', 'etf'), ('iShares Core S&P Small Cap', 'IJR', 'etf'), ('SPDR S&P 600 Small Cap', 'SLY', 'etf'),
    ('Vanguard Mid Cap ETF', 'VO', 'etf'), ('iShares Core S&P Mid Cap', 'IJH', 'etf'), ('SPDR S&P 400 Mid Cap', 'MDY', 'etf'),
    ('Vanguard Total Intl Stock', 'VXUS', 'etf'), ('iShares Core MSCI Intl', 'IEFA', 'etf'), ('SPDR S&P Intl Developed', 'IDV', 'etf'),
    ('Vanguard FTSE Developed', 'VEA', 'etf'), ('iShares MSCI Developed', 'EFA', 'etf'), ('iShares Core MSCI EAFE', 'IEAF', 'etf'),
    ('Vanguard FTSE Emerging', 'VWO', 'etf'), ('iShares MSCI Emerging', 'EEM', 'etf'), ('iShares MSCI Brazil', 'EWZ', 'etf'),
    ('Vanguard Total Bond Market', 'BND', 'etf'), ('iShares Core US Aggregate', 'AGG', 'etf'), ('SPDR Bloomberg Aggregate', 'SCHZ', 'etf'),
    ('Vanguard Short Term Treasury', 'VGSH', 'etf'), ('iShares 1 3 Year Treasury', 'SHY', 'etf'), ('SPDR Bloomberg 1 3 Yr Tsy', 'SHV', 'etf'),
    ('Vanguard Intermediate Term Treasury', 'VGIT', 'etf'), ('iShares 7 10 Year Treasury', 'IEF', 'etf'), ('SPDR Bloomberg 7 10 Yr Tsy', 'IEIF', 'etf'),
    ('Vanguard Long Term Treasury', 'VGLT', 'etf'), ('iShares 20 Year Treasury', 'TLT', 'etf'), ('SPDR Bloomberg 20 Yr Tsy', 'TLTS', 'etf'),
    ('Vanguard High Yield Corporate', 'VWEHX', 'etf'), ('iShares iBoxx High Yield', 'HYG', 'etf'), ('SPDR Bloomberg High Yield', 'JNK', 'etf'),
    ('Vanguard Investment Grade Corp', 'VCIT', 'etf'), ('iShares Investment Grade Corp', 'LQD', 'etf'), ('SPDR Bloomberg Investment Grade', 'LQDB', 'etf'),
    ('iShares 0 5 Year Investment Grade', 'SHVB', 'etf'), ('iShares 5 10 Year Corp', 'IGIB', 'etf'), ('SPDR Bloomberg 5 7 Yr Corp', 'SCHC', 'etf'),
    ('Vanguard Municipal Bond ETF', 'VWAHX', 'etf'), ('iShares National Muni Bond', 'MUB', 'etf'), ('SPDR Nuveen Municipal Bond', 'TFI', 'etf'),
    ('Vanguard Inflation Protected', 'VIPSX', 'etf'), ('iShares TIPS Bond ETF', 'TIP', 'etf'), ('SPDR Bloomberg TIPS Bond', 'SCHP', 'etf'),
    ('Vanguard Emerging Markets Bond', 'VWEM', 'etf'), ('iShares JP Morgan USD', 'EMHY', 'etf'), ('iShares Emerging Market Bond', 'EMHYB', 'etf'),
    ('Vanguard Real Estate ETF', 'VNQ', 'etf'), ('iShares US Real Estate ETF', 'IYR', 'etf'), ('SPDR S&P Real Estate', 'RWR', 'etf'),
    ('Vanguard Health Care ETF', 'VHT', 'etf'), ('iShares US Healthcare ETF', 'IYH', 'etf'), ('SPDR Health Care Select', 'XLV', 'etf'),
    ('Vanguard Technology ETF', 'VGT', 'etf'), ('iShares US Tech ETF', 'IYW', 'etf'), ('SPDR Technology Select Sector', 'XLK', 'etf'),
    ('Vanguard Consumer Disc ETF', 'VCR', 'etf'), ('iShares US Consumer Disc', 'IYC', 'etf'), ('SPDR Consumer Discretionary', 'XLY', 'etf'),
    ('Vanguard Consumer Staples ETF', 'VDC', 'etf'), ('iShares US Consumer Staples', 'IYK', 'etf'), ('SPDR Consumer Staples', 'XLP', 'etf'),
    ('Vanguard Industrials ETF', 'VIS', 'etf'), ('iShares US Industrials ETF', 'IYJ', 'etf'), ('SPDR Industrials Select', 'XLI', 'etf'),
    ('Vanguard Energy ETF', 'VDE', 'etf'), ('iShares US Energy ETF', 'IYE', 'etf'), ('SPDR Energy Select Sector', 'XLE', 'etf'),
    ('Vanguard Materials ETF', 'VAW', 'etf'), ('iShares US Materials ETF', 'IYM', 'etf'), ('SPDR Materials Select Sector', 'XLB', 'etf'),
    ('Vanguard Utilities ETF', 'VPU', 'etf'), ('iShares US Utilities ETF', 'IDU', 'etf'), ('SPDR Utilities Select Sector', 'XLU', 'etf'),
    ('Vanguard Financials ETF', 'VFV', 'etf'), ('iShares US Financials ETF', 'IYF', 'etf'), ('SPDR Financials Select Sector', 'XLF', 'etf'),
    ('Vanguard Telecom ETF', 'VOX', 'etf'), ('iShares US Telecom ETF', 'IYZ', 'etf'), ('SPDR Comm Services Select', 'XLC', 'etf'),
    ('Vanguard Dividend ETF', 'VYM', 'etf'), ('iShares Select Dividend', 'DVYD', 'etf'), ('SPDR S&P Dividend ETF', 'SPDV', 'etf')
  ) AS t(instrument_name, ticker_symbol, asset_class)
)
SELECT instrument_name, ticker_symbol, asset_class
FROM real_instruments
LIMIT 334;

-- model_portfolios (200 rows)
INSERT INTO model_portfolios (name)
SELECT 'Model Portfolio ' || i
FROM generate_series(1, 200) AS i;

-- account_holdings (10,000 rows)
-- account_id cycles through available accounts; instrument_id cycles through 500;
-- as_of_date advances to maintain composite key uniqueness.
INSERT INTO account_holdings (account_id, instrument_id, as_of_date, quantity, status)
SELECT
    ((i % 2500) % 5000) + 1,
    (i % 334) + 1,
    NOW() - INTERVAL '1 day' * (10000 - i),
    (random() * 100000)::INT,
    CASE WHEN random() < 0.8 THEN 'active' ELSE 'inactive' END
FROM generate_series(0, 9999) AS i;

-- account_trades (10,000 rows)
-- Uses account_id, instrument_id, random trade_time within 365 days, and trade status.
INSERT INTO account_trades (account_id, instrument_id, trade_time, trade_type, quantity, price, status)
SELECT
    ((i % 2500) % 5000) + 1,
    (i % 334) + 1,
    NOW() - INTERVAL '1 day' * (random() * 365)::INT,
    CASE WHEN random() < 0.5 THEN 'BUY' ELSE 'SELL' END,
    round((1 + random() * 9999)::NUMERIC, 4),
    round((1 + random() * 999)::NUMERIC, 4),
    CASE (random() * 3)::INT
      WHEN 0 THEN 'pending'
      WHEN 1 THEN 'accepted'
      WHEN 2 THEN 'fulfilled'
      ELSE 'rejected'
    END
FROM generate_series(0, 9999) AS i;

-- model_portfolio_holdings (10,000 rows)
-- Composite key: (model_portfolio_id, instrument_id, effective_date)
-- LCM(200, 500) = 1000, so effective_date advances every 1000 rows to ensure uniqueness
INSERT INTO model_portfolio_holdings (model_portfolio_id, instrument_id, effective_date, target_weight_pct, status)
SELECT
    (i % 200) + 1,
    (i % 334) + 1,
    DATE '2024-01-01' + (i / 1000),
    round((random() * 100)::NUMERIC, 2),
    CASE WHEN random() < 0.8 THEN 'active' ELSE 'inactive' END
FROM generate_series(0, 9999) AS i;

-- account_subscriptions (10,000 rows)
-- Composite key: (account_id, model_portfolio_id, subscription_date)
-- LCM(5000, 200) = 5000, so subscription_date advances every 5000 rows to ensure uniqueness
INSERT INTO account_subscriptions (account_id, model_portfolio_id, subscription_date, status)
SELECT
    (i % 5000) + 1,
    (i % 200) + 1,
    DATE '2024-01-01' + (i / 5000),
    CASE WHEN random() < 0.8 THEN 'active' ELSE 'inactive' END
FROM generate_series(0, 9999) AS i;
