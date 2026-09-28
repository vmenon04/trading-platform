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
INSERT INTO clients (first_name, last_name, email, birth_date)
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
SELECT fn.name, ln.name, LOWER(fn.name || '.' || ln.name) || '.' || ROW_NUMBER() OVER () || '@example.com', DATE '1950-01-01' + (random() * 25000)::INT
FROM first_names fn
CROSS JOIN last_names ln
LIMIT 1000;

-- accounts (5,000 rows; 5 account types, roughly 1000 of each)
INSERT INTO accounts (account_type, balance)
SELECT CASE (i % 5)
  WHEN 0 THEN 'cash'
  WHEN 1 THEN 'margin'
  WHEN 2 THEN 'retirement'
  WHEN 3 THEN 'investment'
  ELSE 'savings'
END,
  round((random() * 50000)::NUMERIC(14, 4), 2)
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
    ('Apple Inc', 'AAPL', 'STOCK'), ('Microsoft Corporation', 'MSFT', 'STOCK'), ('Amazon.com Inc', 'AMZN', 'STOCK'),
    ('Alphabet Inc', 'GOOGL', 'STOCK'), ('Tesla Inc', 'TSLA', 'STOCK'), ('NVIDIA Corporation', 'NVDA', 'STOCK'),
    ('Meta Platforms Inc', 'META', 'STOCK'), ('Berkshire Hathaway', 'BRK', 'STOCK'), ('JPMorgan Chase', 'JPM', 'STOCK'),
    ('Visa Inc', 'V', 'STOCK'), ('Mastercard Inc', 'MA', 'STOCK'), ('Procter & Gamble', 'PG', 'STOCK'),
    ('Johnson & Johnson', 'JNJ', 'STOCK'), ('Coca-Cola Company', 'KO', 'STOCK'), ('Walmart Inc', 'WMT', 'STOCK'),
    ('Boeing Company', 'BA', 'STOCK'), ('Intel Corporation', 'INTC', 'STOCK'), ('AMD Inc', 'AMD', 'STOCK'),
    ('Netflix Inc', 'NFLX', 'STOCK'), ('Adobe Inc', 'ADBE', 'STOCK'), ('Cisco Systems', 'CSCO', 'STOCK'),
    ('Oracle Corporation', 'ORCL', 'STOCK'), ('Salesforce Inc', 'CRM', 'STOCK'), ('IBM Corporation', 'IBM', 'STOCK'),
    ('Chevron Corporation', 'CVX', 'STOCK'), ('ExxonMobil Corporation', 'XOM', 'STOCK'), ('ConocoPhillips', 'COP', 'STOCK'),
    ('Schlumberger Ltd', 'SLB', 'STOCK'), ('Valero Energy', 'VLO', 'STOCK'), ('Antero Midstream', 'AM', 'STOCK'),
    ('Home Depot Inc', 'HD', 'STOCK'), ('Lowes Companies', 'LOW', 'STOCK'), ('Target Corporation', 'TGT', 'STOCK'),
    ('Best Buy Co', 'BBY', 'STOCK'), ('Gap Inc', 'GPS', 'STOCK'), ('Nike Inc', 'NKE', 'STOCK'),
    ('Adidas AG', 'ADSY', 'STOCK'), ('Starbucks Corporation', 'SBUX', 'STOCK'), ('McDonalds Corp', 'MCD', 'STOCK'),
    ('Yum Brands', 'YUM', 'STOCK'), ('Dominos Pizza', 'DPZ', 'STOCK'), ('Chipotle Mexican Grill', 'CMG', 'STOCK'),
    ('Uber Technologies', 'UBER', 'STOCK'), ('Lyft Inc', 'LYFT', 'STOCK'), ('AirBnB Inc', 'ABNB', 'STOCK'),
    ('PayPal Holdings', 'PYPL', 'STOCK'), ('Square Inc', 'SQ', 'STOCK'), ('Block Inc', 'BLOCK', 'STOCK'),
    ('Stripe Holdings', 'STRIPE', 'STOCK'), ('Shopify Inc', 'SHOP', 'STOCK'), ('Docusign Inc', 'DOCU', 'STOCK'),
    ('Zoom Video Communications', 'ZM', 'STOCK'), ('ServiceNow Inc', 'NOW', 'STOCK'), ('Workday Inc', 'WDAY', 'STOCK'),
    ('Synopsys Inc', 'SNPS', 'STOCK'), ('Cadence Design', 'CDNS', 'STOCK'), ('Asml Holdings', 'ASML', 'STOCK'),
    ('Broadcom Inc', 'AVGO', 'STOCK'), ('Qualcomm Inc', 'QCOM', 'STOCK'), ('Applied Materials', 'AMAT', 'STOCK'),
    ('Lam Research', 'LRCX', 'STOCK'), ('ASAP Semiconductor', 'ASAP', 'STOCK'), ('Analog Devices', 'ADI', 'STOCK'),
    ('Monolithic Power Systems', 'MPWR', 'STOCK'), ('Advanced Energy Industries', 'AEIS', 'STOCK'), ('Marvell Technology', 'MRVL', 'STOCK'),
    ('Micron Technology', 'MU', 'STOCK'), ('Western Digital', 'WDC', 'STOCK'), ('Corsair Gaming', 'CRSR', 'STOCK'),
    ('Turtle Beach', 'HEAR', 'STOCK'), ('SoundThetics Inc', 'SNTS', 'STOCK'), ('Dolby Laboratories', 'DLB', 'STOCK'),
    ('CVS Health Corporation', 'CVS', 'STOCK'), ('Walgreens Boots Alliance', 'WBA', 'STOCK'), ('Pfizer Inc', 'PFE', 'STOCK'),
    ('Moderna Inc', 'MRNA', 'STOCK'), ('AstraZeneca PLC', 'AZN', 'STOCK'), ('Merck Co', 'MRK', 'STOCK'),
    ('Eli Lilly and Company', 'LLY', 'STOCK'), ('Thermo Fisher Scientific', 'TMO', 'STOCK'), ('Illumina Inc', 'ILMN', 'STOCK'),
    ('Qiagen NV', 'QGEN', 'STOCK'), ('BioRad Laboratories', 'BIO', 'STOCK'), ('Avantor Inc', 'AVTR', 'STOCK'),
    ('Charles River Labs', 'CRL', 'STOCK'), ('Zoetis Inc', 'ZTS', 'STOCK'), ('Neogen Corporation', 'NEOG', 'STOCK'),
    -- Bonds (100)
    ('US Treasury 2Y', 'UST2', 'BOND'), ('US Treasury 5Y', 'UST5', 'BOND'), ('US Treasury 10Y', 'UST10', 'BOND'),
    ('US Treasury 20Y', 'UST20', 'BOND'), ('US Treasury 30Y', 'UST30', 'BOND'), ('UK Gilts 10Y', 'GBL10', 'BOND'),
    ('German Bunds 10Y', 'BUN10', 'BOND'), ('Japanese JGB 10Y', 'JGB10', 'BOND'), ('Canadian Bond 10Y', 'CAD10', 'BOND'),
    ('Australian Bond 10Y', 'AUD10', 'BOND'), ('Swiss Bond 10Y', 'CHB10', 'BOND'), ('Sweden Bond 10Y', 'SEB10', 'BOND'),
    ('Norway Bond 10Y', 'NOB10', 'BOND'), ('New Zealand Bond 10Y', 'NZB10', 'BOND'), ('Singapore Bond 10Y', 'SGB10', 'BOND'),
    ('Hong Kong Bond 10Y', 'HKB10', 'BOND'), ('India Bond 10Y', 'INB10', 'BOND'), ('Brazil Bond 10Y', 'BRB10', 'BOND'),
    ('Mexico Bond 10Y', 'MXB10', 'BOND'), ('Russia Bond 10Y', 'RUB10', 'BOND'), ('South Africa Bond 10Y', 'ZAB10', 'BOND'),
    ('Egypt Bond 10Y', 'EGB10', 'BOND'), ('Nigeria Bond 10Y', 'NGB10', 'BOND'), ('UAE Bond 10Y', 'AED10', 'BOND'),
    ('Saudi Bond 10Y', 'SAB10', 'BOND'), ('Israel Bond 10Y', 'ILB10', 'BOND'), ('Turkey Bond 10Y', 'TRB10', 'BOND'),
    ('Greece Bond 10Y', 'GRB10', 'BOND'), ('Portugal Bond 10Y', 'PTB10', 'BOND'), ('Spain Bond 10Y', 'ESB10', 'BOND'),
    ('Italy Bond 10Y', 'ITB10', 'BOND'), ('Ireland Bond 10Y', 'IEB10', 'BOND'), ('Belgium Bond 10Y', 'BEB10', 'BOND'),
    ('France Bond 10Y', 'FRB10', 'BOND'), ('Netherlands Bond 10Y', 'NLB10', 'BOND'), ('Austria Bond 10Y', 'ATB10', 'BOND'),
    ('Corporate AAA Bond', 'CAAA', 'BOND'), ('Corporate AA Bond', 'CAAB', 'BOND'), ('Corporate A Bond', 'CACA', 'BOND'),
    ('Corporate BBB Bond', 'CBBB', 'BOND'), ('Corporate BB Bond', 'CBBC', 'BOND'), ('Corporate B Bond', 'CBCD', 'BOND'),
    ('High Yield Bond 1', 'HYB1', 'BOND'), ('High Yield Bond 2', 'HYB2', 'BOND'), ('High Yield Bond 3', 'HYB3', 'BOND'),
    ('Municipal Bond 1', 'MUB1', 'BOND'), ('Municipal Bond 2', 'MUB2', 'BOND'), ('Municipal Bond 3', 'MUB3', 'BOND'),
    ('Inflation Bond 1', 'INB1', 'BOND'), ('Inflation Bond 2', 'INB2', 'BOND'), ('Floating Rate Bond 1', 'FLB1', 'BOND'),
    ('Floating Rate Bond 2', 'FLB2', 'BOND'), ('Convertible Bond 1', 'CVB1', 'BOND'), ('Convertible Bond 2', 'CVB2', 'BOND'),
    ('Green Bond 1', 'GRB1', 'BOND'), ('Green Bond 2', 'GRB2', 'BOND'), ('ESG Bond 1', 'ESB1', 'BOND'),
    ('ESG Bond 2', 'ESB2', 'BOND'), ('Sustainability Bond 1', 'SUS1', 'BOND'), ('Sustainability Bond 2', 'SUS2', 'BOND'),
    ('Emerging Market Bond 1', 'EMB1', 'BOND'), ('Emerging Market Bond 2', 'EMB2', 'BOND'), ('Emerging Market Bond 3', 'EMB3', 'BOND'),
    -- Cryptocurrencies (150)
    ('Bitcoin', 'BTC', 'CRYPTO'), ('Ethereum', 'ETH', 'CRYPTO'), ('Tether', 'USDT', 'CRYPTO'), ('USD Coin', 'USDC', 'CRYPTO'),
    ('Binance Coin', 'BNB', 'CRYPTO'), ('Solana', 'SOL', 'CRYPTO'), ('Ripple', 'XRP', 'CRYPTO'), ('Polkadot', 'DOT', 'CRYPTO'),
    ('Dogecoin', 'DOGE', 'CRYPTO'), ('Cardano', 'ADA', 'CRYPTO'), ('Polygon', 'MATIC', 'CRYPTO'), ('Litecoin', 'LTC', 'CRYPTO'),
    ('Bitcoin Cash', 'BCH', 'CRYPTO'), ('Stellar', 'XLM', 'CRYPTO'), ('Chainlink', 'LINK', 'CRYPTO'), ('Cosmos', 'ATOM', 'CRYPTO'),
    ('Monero', 'XMR', 'CRYPTO'), ('Zcash', 'ZEC', 'CRYPTO'), ('Dash', 'DASH', 'CRYPTO'), ('NEO', 'NEO', 'CRYPTO'),
    ('EOS', 'EOS', 'CRYPTO'), ('Tron', 'TRX', 'CRYPTO'), ('Iota', 'MIOTA', 'CRYPTO'), ('Vechain', 'VET', 'CRYPTO'),
    ('Theta', 'THETA', 'CRYPTO'), ('Algorand', 'ALGO', 'CRYPTO'), ('Flow', 'FLOW', 'CRYPTO'), ('Fantom', 'FTM', 'CRYPTO'),
    ('Avalanche', 'AVAX', 'CRYPTO'), ('Harmony', 'ONE', 'CRYPTO'), ('Elrond', 'EGLD', 'CRYPTO'), ('Zilliqa', 'ZIL', 'CRYPTO'),
    ('Tezos', 'XTZ', 'CRYPTO'), ('Celo', 'CELO', 'CRYPTO'), ('Bitcoin SV', 'BSV', 'CRYPTO'), ('Huobi Token', 'HT', 'CRYPTO'),
    ('OKB Token', 'OKB', 'CRYPTO'), ('Uniswap', 'UNI', 'CRYPTO'), ('Aave', 'AAVE', 'CRYPTO'), ('Maker', 'MKR', 'CRYPTO'),
    ('Curve DAO', 'CRV', 'CRYPTO'), ('Lido DAO', 'LDO', 'CRYPTO'), ('Convex', 'CVX', 'CRYPTO'), ('Balancer', 'BAL', 'CRYPTO'),
    ('Yearn Finance', 'YFI', 'CRYPTO'), ('SushiSwap', 'SUSHI', 'CRYPTO'), ('PancakeSwap', 'CAKE', 'CRYPTO'), ('Dydx', 'DYDX', 'CRYPTO'),
    ('Compound', 'COMP', 'CRYPTO'), ('Flux', 'FLUX', 'CRYPTO'), ('Ankr', 'ANKR', 'CRYPTO'), ('Akash Network', 'AKT', 'CRYPTO'),
    ('Internet Computer', 'ICP', 'CRYPTO'), ('Filecoin', 'FIL', 'CRYPTO'), ('Arweave', 'AR', 'CRYPTO'), ('Helium', 'HNT', 'CRYPTO'),
    ('Render Network', 'RNDR', 'CRYPTO'), ('Livepeer', 'LPT', 'CRYPTO'), ('Immutable X', 'IMX', 'CRYPTO'), ('Aptos', 'APT', 'CRYPTO'),
    ('Sui', 'SUI', 'CRYPTO'), ('SEI', 'SEI', 'CRYPTO'), ('Pyth Network', 'PYTH', 'CRYPTO'), ('Wormhole', 'WORM', 'CRYPTO'),
    ('Pendle', 'PENDLE', 'CRYPTO'), ('Arbitrum', 'ARB', 'CRYPTO'), ('Optimism', 'OP', 'CRYPTO'), ('Starknet', 'STRK', 'CRYPTO'),
    ('Linea', 'LINEA', 'CRYPTO'), ('Blast', 'BLAST', 'CRYPTO'), ('Scroll', 'SCRL', 'CRYPTO'), ('Mantle', 'MNT', 'CRYPTO'),
    ('Manta Network', 'MANTA', 'CRYPTO'), ('Ternoa', 'CAPS', 'CRYPTO'), ('Phala Network', 'PHA', 'CRYPTO'), ('Kusama', 'KSM', 'CRYPTO'),
    ('Moonriver', 'MOVR', 'CRYPTO'), ('Hedera', 'HBAR', 'CRYPTO'), ('Nervos', 'CKB', 'CRYPTO'), ('Stacks', 'STX', 'CRYPTO'),
    ('Dfinity', 'DFNT', 'CRYPTO'), ('Mina Protocol', 'MINA', 'CRYPTO'), ('Astar', 'ASTR', 'CRYPTO'), ('OKX Exchange Token', 'OKT', 'CRYPTO'),
    ('Gate Token', 'GT', 'CRYPTO'), ('Kucoin Token', 'KCS', 'CRYPTO'), ('Bybit Token', 'BIT', 'CRYPTO'), ('Kraken Token', 'KRAK', 'CRYPTO'),
    -- ETFs (150)
    ('Vanguard S&P 500 ETF', 'VOO', 'ETF'), ('iShares Core S&P 500 ETF', 'IVV', 'ETF'), ('SPDR S&P 500 ETF Trust', 'SPY', 'ETF'),
    ('Vanguard Total US Stock', 'VTI', 'ETF'), ('iShares Core US Total', 'ITOT', 'ETF'), ('Schwab US Total Stock', 'SWUS', 'ETF'),
    ('Vanguard Dividend Appreciation', 'VIG', 'ETF'), ('iShares Select Dividend', 'DVY', 'ETF'), ('SPDR S&P Dividend', 'SDY', 'ETF'),
    ('Vanguard Growth ETF', 'VUG', 'ETF'), ('iShares Russell 1000 Growth', 'IWF', 'ETF'), ('SPDR S&P 500 Growth', 'SPLG', 'ETF'),
    ('Vanguard Value ETF', 'VTV', 'ETF'), ('iShares Russell 1000 Value', 'IWD', 'ETF'), ('SPDR S&P 500 Value', 'SPYV', 'ETF'),
    ('Vanguard Small Cap ETF', 'VB', 'ETF'), ('iShares Core S&P Small Cap', 'IJR', 'ETF'), ('SPDR S&P 600 Small Cap', 'SLY', 'ETF'),
    ('Vanguard Mid Cap ETF', 'VO', 'ETF'), ('iShares Core S&P Mid Cap', 'IJH', 'ETF'), ('SPDR S&P 400 Mid Cap', 'MDY', 'ETF'),
    ('Vanguard Total Intl Stock', 'VXUS', 'ETF'), ('iShares Core MSCI Intl', 'IEFA', 'ETF'), ('SPDR S&P Intl Developed', 'IDV', 'ETF'),
    ('Vanguard FTSE Developed', 'VEA', 'ETF'), ('iShares MSCI Developed', 'EFA', 'ETF'), ('iShares Core MSCI EAFE', 'IEAF', 'ETF'),
    ('Vanguard FTSE Emerging', 'VWO', 'ETF'), ('iShares MSCI Emerging', 'EEM', 'ETF'), ('iShares MSCI Brazil', 'EWZ', 'ETF'),
    ('Vanguard Total Bond Market', 'BND', 'ETF'), ('iShares Core US Aggregate', 'AGG', 'ETF'), ('SPDR Bloomberg Aggregate', 'SCHZ', 'ETF'),
    ('Vanguard Short Term Treasury', 'VGSH', 'ETF'), ('iShares 1 3 Year Treasury', 'SHY', 'ETF'), ('SPDR Bloomberg 1 3 Yr Tsy', 'SHV', 'ETF'),
    ('Vanguard Intermediate Term Treasury', 'VGIT', 'ETF'), ('iShares 7 10 Year Treasury', 'IEF', 'ETF'), ('SPDR Bloomberg 7 10 Yr Tsy', 'IEIF', 'ETF'),
    ('Vanguard Long Term Treasury', 'VGLT', 'ETF'), ('iShares 20 Year Treasury', 'TLT', 'ETF'), ('SPDR Bloomberg 20 Yr Tsy', 'TLTS', 'ETF'),
    ('Vanguard High Yield Corporate', 'VWEHX', 'ETF'), ('iShares iBoxx High Yield', 'HYG', 'ETF'), ('SPDR Bloomberg High Yield', 'JNK', 'ETF'),
    ('Vanguard Investment Grade Corp', 'VCIT', 'ETF'), ('iShares Investment Grade Corp', 'LQD', 'ETF'), ('SPDR Bloomberg Investment Grade', 'LQDB', 'ETF'),
    ('iShares 0 5 Year Investment Grade', 'SHVB', 'ETF'), ('iShares 5 10 Year Corp', 'IGIB', 'ETF'), ('SPDR Bloomberg 5 7 Yr Corp', 'SCHC', 'ETF'),
    ('Vanguard Municipal Bond ETF', 'VWAHX', 'ETF'), ('iShares National Muni Bond', 'MUB', 'ETF'), ('SPDR Nuveen Municipal Bond', 'TFI', 'ETF'),
    ('Vanguard Inflation Protected', 'VIPSX', 'ETF'), ('iShares TIPS Bond ETF', 'TIP', 'ETF'), ('SPDR Bloomberg TIPS Bond', 'SCHP', 'ETF'),
    ('Vanguard Emerging Markets Bond', 'VWEM', 'ETF'), ('iShares JP Morgan USD', 'EMHY', 'ETF'), ('iShares Emerging Market Bond', 'EMHYB', 'ETF'),
    ('Vanguard Real Estate ETF', 'VNQ', 'ETF'), ('iShares US Real Estate ETF', 'IYR', 'ETF'), ('SPDR S&P Real Estate', 'RWR', 'ETF'),
    ('Vanguard Health Care ETF', 'VHT', 'ETF'), ('iShares US Healthcare ETF', 'IYH', 'ETF'), ('SPDR Health Care Select', 'XLV', 'ETF'),
    ('Vanguard Technology ETF', 'VGT', 'ETF'), ('iShares US Tech ETF', 'IYW', 'ETF'), ('SPDR Technology Select Sector', 'XLK', 'ETF'),
    ('Vanguard Consumer Disc ETF', 'VCR', 'ETF'), ('iShares US Consumer Disc', 'IYC', 'ETF'), ('SPDR Consumer Discretionary', 'XLY', 'ETF'),
    ('Vanguard Consumer Staples ETF', 'VDC', 'ETF'), ('iShares US Consumer Staples', 'IYK', 'ETF'), ('SPDR Consumer Staples', 'XLP', 'ETF'),
    ('Vanguard Industrials ETF', 'VIS', 'ETF'), ('iShares US Industrials ETF', 'IYJ', 'ETF'), ('SPDR Industrials Select', 'XLI', 'ETF'),
    ('Vanguard Energy ETF', 'VDE', 'ETF'), ('iShares US Energy ETF', 'IYE', 'ETF'), ('SPDR Energy Select Sector', 'XLE', 'ETF'),
    ('Vanguard Materials ETF', 'VAW', 'ETF'), ('iShares US Materials ETF', 'IYM', 'ETF'), ('SPDR Materials Select Sector', 'XLB', 'ETF'),
    ('Vanguard Utilities ETF', 'VPU', 'ETF'), ('iShares US Utilities ETF', 'IDU', 'ETF'), ('SPDR Utilities Select Sector', 'XLU', 'ETF'),
    ('Vanguard Financials ETF', 'VFV', 'ETF'), ('iShares US Financials ETF', 'IYF', 'ETF'), ('SPDR Financials Select Sector', 'XLF', 'ETF'),
    ('Vanguard Telecom ETF', 'VOX', 'ETF'), ('iShares US Telecom ETF', 'IYZ', 'ETF'), ('SPDR Comm Services Select', 'XLC', 'ETF'),
    ('Vanguard Dividend ETF', 'VYM', 'ETF'), ('iShares Select Dividend', 'DVYD', 'ETF'), ('SPDR S&P Dividend ETF', 'SPDV', 'ETF')
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
    CASE WHEN random() < 0.8 THEN 'ACTIVE' ELSE 'INACTIVE' END
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
      WHEN 0 THEN 'PENDING'
      WHEN 1 THEN 'ACCEPTED'
      WHEN 2 THEN 'FULFILLED'
      ELSE 'REJECTED'
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
    CASE WHEN random() < 0.8 THEN 'ACTIVE' ELSE 'INACTIVE' END
FROM generate_series(0, 9999) AS i;

-- account_subscriptions (10,000 rows)
-- Composite key: (account_id, model_portfolio_id, subscription_date)
-- LCM(5000, 200) = 5000, so subscription_date advances every 5000 rows to ensure uniqueness
INSERT INTO account_subscriptions (account_id, model_portfolio_id, subscription_date, status)
SELECT
    (i % 5000) + 1,
    (i % 200) + 1,
    DATE '2024-01-01' + (i / 5000),
    CASE WHEN random() < 0.8 THEN 'ACTIVE' ELSE 'INACTIVE' END
FROM generate_series(0, 9999) AS i;
