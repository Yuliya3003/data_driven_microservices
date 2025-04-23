import requests
import os

def fetch_prometheus_metrics():
    try:
        response = requests.get('http://localhost:9090/api/v1/query', params={'query': 'http_server_requests_seconds_sum'})
        data = response.json()
        return data['data']['result']
    except Exception as e:
        print(f"Error fetching metrics: {e}")
        return []

def generate_html(metrics):
    html_content = """
    <!DOCTYPE html>
    <html lang="ru">
    <head>
        <meta charset="UTF-8">
        <title>Метрики производительности TaskManager</title>
        <style>
            body { font-family: Arial, sans-serif; margin: 40px; }
            h1 { color: #333; }
            table { border-collapse: collapse; width: 100%; }
            th, td { border: 1px solid #ddd; padding: 8px; text-align: left; }
            th { background-color: #f2f2f2; }
        </style>
    </head>
    <body>
        <h1>Метрики производительности TaskManager</h1>
        <table>
            <tr>
                <th>Сервис</th>
                <th>Метрика</th>
                <th>Значение</th>
            </tr>
    """

    for metric in metrics:
        service = metric['metric'].get('job', 'unknown')
        metric_name = metric['metric'].get('__name__', 'unknown')
        value = metric['value'][1]
        html_content += f"""
            <tr>
                <td>{service}</td>
                <td>{metric_name}</td>
                <td>{value}</td>
            </tr>
        """

    html_content += """
        </table>
    </body>
    </html>
    """
    return html_content

def save_html(content):
    os.makedirs('public', exist_ok=True)
    with open('public/index.html', 'w') as f:
        f.write(content)

if __name__ == '__main__':
    metrics = fetch_prometheus_metrics()
    html_content = generate_html(metrics)
    save_html(html_content)