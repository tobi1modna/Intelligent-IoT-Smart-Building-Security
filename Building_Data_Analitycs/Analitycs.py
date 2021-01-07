import os
import json
import pandas as pd
import matplotlib.pyplot as plt

data_folder = r"C:\Users\zanna\Desktop\data-analysis-playground-master\data"
data_file = "Pir Sensor.json"

plt.style.use('seaborn')

file_path = os.path.join(data_folder, data_file)

senml_record_list = []
with open(file_path) as file:
    for line in file.readlines():
        json_senml_pack = json.loads(line)
        senml_record_list.append(json_senml_pack[0])

df = pd.DataFrame(senml_record_list) 
df['datetime'] = pd.to_datetime(df['t'], unit='ms')




#Grafico Lineare Camera
plt1 = plt.subplot(2,2,1)
ss = df[df['u'].isin(["Num"])]
sorted_df = ss.sort_values(by='t')
plt1.plot(sorted_df['datetime'], sorted_df['v'])
plt.xticks(rotation=70)
plt1.title.set_text('Camera Sensor')
plt1.set_xlabel('Time')
plt1.set_ylabel('Measurements Count')



#Bar Graph Pir
plt2 = plt.subplot(2,2,2)
bf3=df[df['bn'].str.contains("alarm")]
bf2 = bf3[bf3['vb'].isin([True])]
bf2['VEROdt'] = bf2['datetime'].dt.date
cc = bf2['VEROdt'].value_counts()
plt.xticks(range(len(cc)), cc.index)
plt.bar(range(len(cc)), cc)
plt.xticks(rotation=70)
plt2.title.set_text('Pir Sensor')
plt2.set_xlabel('Days')
plt2.set_ylabel('Measurements Count')



#Grafico lineare Allarme
plt3 = plt.subplot(2,2,3)
pp=df[df['bn'].str.contains("alarm")]
pp['VEROdt'] = pp['datetime'].dt.date
plt.scatter(pp['VEROdt'],pp['vb'],c='#ff0000',s=500)
plt3.title.set_text('ALLARME')
plt3.set_xlabel('Days')
plt3.set_ylabel('Measurements Count')


#GraficoLuci
plt4 = plt.subplot(2,2,4)
plt4.title.set_text('Luci')
plt4.set_xlabel('Days')
plt4.set_ylabel('Measurements Count')
uu=df[df['bn'].str.contains("light")]
uu2 = uu[uu['vb'].isin([True])]
uu2['VEROdt'] = uu2['datetime'].dt.date
rr = uu2['VEROdt'].value_counts()
print(rr)
plt.xticks(range(len(rr)), rr.index)
plt.bar(range(len(rr)), rr)
plt.xticks(rotation=70)


plt.show()