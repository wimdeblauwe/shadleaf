/*
  A port of shadcn/ui's ui/chart.tsx: ChartContainer, ChartTooltipContent and ChartLegendContent with the same
  structure, with classes (components/chart.css and the skins) instead of Tailwind utilities. What differs:
  - the config is built from the series parts (sl:chart-bar, -line, -area): their key, label and colour;
  - shadcn's ChartStyle, a <style> element a strict Content-Security-Policy blocks, is gone: sl-chart.jsx sets the
    --color-<key> properties through CSSOM;
  - shadcn's formatter and labelFormatter functions are named formats (formats.js), as attributes of the parts;
  - labelKey also takes the label from that column of the hovered row when no series has that key, for a label the
    server formatted.
*/
import {createContext, useContext, useMemo} from 'react';
import {ResponsiveContainer} from 'recharts';

const INITIAL_DIMENSION = {width: 320, height: 200};

const ChartContext = createContext(null);

function useChart() {
  const context = useContext(ChartContext);
  if (!context) {
    throw new Error('useChart must be used within a <ChartContainer />');
  }
  return context;
}

export function ChartContainer({config, children}) {
  return (
    <ChartContext.Provider value={{config}}>
      <div className="chart-container">
        <ResponsiveContainer initialDimension={INITIAL_DIMENSION}>{children}</ResponsiveContainer>
      </div>
    </ChartContext.Provider>
  );
}

export function ChartTooltipContent({
  active,
  payload,
  indicator = 'dot',
  hideLabel = false,
  hideIndicator = false,
  label,
  labelKey,
  formatLabel,
  formatValue
}) {
  const {config} = useChart();

  const tooltipLabel = useMemo(() => {
    if (hideLabel || !payload?.length) {
      return null;
    }
    const [item] = payload;
    const key = `${labelKey ?? item?.dataKey ?? item?.name ?? 'value'}`;
    const itemConfig = getPayloadConfigFromPayload(config, item, key);
    let value;
    if (!labelKey && typeof label === 'string') {
      value = config[label]?.label ?? formatLabel(label);
    } else if (!labelKey) {
      value = itemConfig?.label;
    } else {
      value = itemConfig?.label ?? formatLabel(item?.payload?.[labelKey]);
    }
    if (value == null || value === '') {
      return null;
    }
    return <div className="chart-tooltip-label">{value}</div>;
  }, [label, payload, hideLabel, config, labelKey, formatLabel]);

  if (!active || !payload?.length) {
    return null;
  }

  const nestLabel = payload.length === 1 && indicator !== 'dot';

  return (
    <div className="chart-tooltip-content">
      {!nestLabel ? tooltipLabel : null}
      <div className="chart-tooltip-items">
        {payload
          .filter(item => item.type !== 'none')
          .map((item, index) => {
            const key = `${item.name ?? item.dataKey ?? 'value'}`;
            const itemConfig = getPayloadConfigFromPayload(config, item, key);
            const indicatorColor = item.payload?.fill ?? item.color;
            return (
              <div key={index} className="chart-tooltip-item" data-indicator={indicator}>
                {!hideIndicator && (
                  <div
                    className="chart-tooltip-indicator"
                    data-indicator={indicator}
                    data-nested={nestLabel ? 'true' : undefined}
                    style={{'--indicator-color': indicatorColor}}
                  />
                )}
                <div className="chart-tooltip-body" data-nested={nestLabel ? 'true' : undefined}>
                  <div className="chart-tooltip-name">
                    {nestLabel ? tooltipLabel : null}
                    <span>{itemConfig?.label ?? item.name}</span>
                  </div>
                  {item.value != null && (
                    <span className="chart-tooltip-value">{String(formatValue(item.value))}</span>
                  )}
                </div>
              </div>
            );
          })}
      </div>
    </div>
  );
}

export function ChartLegendContent({payload, verticalAlign = 'bottom'}) {
  const {config} = useChart();
  if (!payload?.length) {
    return null;
  }
  return (
    <div className="chart-legend-content" data-align={verticalAlign}>
      {payload
        .filter(item => item.type !== 'none')
        .map((item, index) => {
          const key = `${item.dataKey ?? 'value'}`;
          const itemConfig = getPayloadConfigFromPayload(config, item, key);
          return (
            <div key={index} className="chart-legend-item">
              <div className="chart-legend-indicator" style={{backgroundColor: item.color}}/>
              {itemConfig?.label}
            </div>
          );
        })}
    </div>
  );
}

// Helper to extract item config from a payload.
function getPayloadConfigFromPayload(config, payload, key) {
  if (typeof payload !== 'object' || payload === null) {
    return undefined;
  }
  const payloadPayload = typeof payload.payload === 'object' && payload.payload !== null ? payload.payload : undefined;
  let configLabelKey = key;
  if (typeof payload[key] === 'string') {
    configLabelKey = payload[key];
  } else if (payloadPayload && typeof payloadPayload[key] === 'string') {
    configLabelKey = payloadPayload[key];
  }
  return configLabelKey in config ? config[configLabelKey] : config[key];
}
