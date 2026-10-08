from pathlib import Path
import csv, matplotlib
matplotlib.use('Agg')
import matplotlib.pyplot as plt
import numpy as np
root=Path(__file__).resolve().parent
plt.rcParams.update({'font.family':'DejaVu Sans','font.size':11,'svg.fonttype':'none','axes.spines.top':False,'axes.spines.right':False})
rows=list(csv.DictReader((root/'gc-allocation-baselines.csv').open()))
fig,axs=plt.subplots(1,2,figsize=(13,5.4),layout='constrained')
x=np.arange(len(rows));labels=['macOS\nARM64','Linux x64\nEPYC 7763','Linux ARM64\nNeoverse N2','Windows x64\nAMD family 25']
for ax,metric,title in zip(axs,['time','memory'],['Allocation time','Peak process memory']):
 for shift,phase,color in [(-.19,'before','#8c9aaf'),(.19,'after','#166baf')]:
  vals=[float(r[f'{phase}_{metric}_ratio']) for r in rows]
  bars=ax.bar(x+shift,vals,.36,label=phase.title(),color=color)
  ax.bar_label(bars,fmt='%.3f',fontsize=9)
 ax.axhline(1,color='#aa5435',ls='--',label='JDK 25 = 1')
 ax.set_xticks(x,labels);ax.set_title(title);ax.set_ylabel('ParparVM / JDK 25, lower is better');ax.set_ylim(0, max(1, max(float(r[f'{phase}_{metric}_ratio']) for r in rows for phase in ['before','after']))*1.27);ax.legend(fontsize=9)
fig.suptitle('objectAllocation: historical gate calibration changes',fontsize=17)
fig.text(.5,-.025,'PR #5940 calibration overlay • not a new controlled experiment • unchanged values may be carried forward',ha='center',fontsize=10)
fig.savefig(root/'gc-ratios.svg',bbox_inches='tight');fig.savefig(root/'gc-ratios.png',dpi=150,bbox_inches='tight')
plt.close(fig)
fig,ax=plt.subplots(figsize=(10,4.8),layout='constrained');a=np.linspace(.08,1,100)
ax.plot(a,1/(a+.2)/5,label='Frequent collection work',lw=3,color='#166baf')
ax.plot(a,.18+.6*a**1.6,label='Retained memory / later scan work',lw=3,color='#b15737')
ax.set_xticks([.12,.88],['Smaller budget','Larger budget']);ax.set_yticks([]);ax.set_xlabel('Young-generation memory budget');ax.set_ylabel('Relative cost (illustrative)');ax.legend();ax.set_title('The direction of the trade, not measured performance',fontsize=16)
fig.savefig(root/'gc-budget.svg');fig.savefig(root/'gc-budget.png',dpi=140)
