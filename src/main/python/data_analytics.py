# Business insights


# Trade volume by time period

# trades = pd.read_csv("trades.csv", parse_dates=["trade_date"])
# by_date = trades.set_index("trade_date")
# by_date.resample("M").sum()  # Example: monthly trade volume

# Trade value/volume per customer

# join client_accounts and account_trades
# client_trades = account_trades.merge(client_accounts, on="account_id")
# by_customer = client_trades.groupby("customer_id").sum()  # Example: trade volume per customer
# by_customer = client_trades.groupby("customer_id")['value'].sum()  # Example: trade volume per customer

# Trade value/volume by asset class

# by_asset_class = trades.groupby("asset_class").sum()  # Example: trade volume per asset class
# by_asset_class = trades.groupby("asset_class")['value'].sum()  # Example: trade value per asset class

# Trade volume by buy/sell

# by_trade_type = trades.groupby("trade_type").sum()  # Example: trade volume by buy/sell
# by_trade_type = trades.groupby("trade_type")['value'].sum()  # Example: trade value by buy/sell